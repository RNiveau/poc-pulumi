package myproject;

import com.pulumi.Context;
import com.pulumi.aws.ec2.*;
import com.pulumi.aws.ec2.inputs.DefaultSecurityGroupEgressArgs;
import com.pulumi.aws.ec2.inputs.DefaultSecurityGroupIngressArgs;
import com.pulumi.aws.ec2.inputs.SecurityGroupEgressArgs;
import com.pulumi.aws.ec2.inputs.SecurityGroupIngressArgs;
import com.pulumi.aws.vpc.SecurityGroupIngressRuleArgs;
import com.pulumi.core.Output;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Network {

    private Vpc vpc;
    private Subnet subnetRdsA;
    private Subnet subnetRdsB;
    private Subnet subnetEks;

    private Subnet subnetEc2;

    private Output<List<String>> subnetsId;

    public Network createNetwork(Context ctx) {

        vpc = new Vpc("pulumi", VpcArgs.builder()
                .cidrBlock("10.0.0.0/20")
                .build());

        var internetGateway = new InternetGateway("pulumi-gw", InternetGatewayArgs.Empty);

        new InternetGatewayAttachment("pulumi-gw-attachment",
                InternetGatewayAttachmentArgs.builder()
                        .vpcId(vpc.id())
                        .internetGatewayId(internetGateway.id()).build());


        createSubnets(ctx);
        createRouteTable(internetGateway);
        createSecurityGroup();

        ctx.export("vpc_id", vpc.id());
        return this;
    }

    private void createSecurityGroup() {
        DefaultSecurityGroupIngressArgs tcpRule = DefaultSecurityGroupIngressArgs.builder()
                .cidrBlocks("0.0.0.0/0")
                .fromPort(22)
                .toPort(22)
                .protocol("tcp").build();

        DefaultSecurityGroupIngressArgs selfIngressRule = DefaultSecurityGroupIngressArgs.builder()
                .self(true)
                .fromPort(0)
                .toPort(0)
                .protocol("ALL")
                .build();

        DefaultSecurityGroupEgressArgs all = DefaultSecurityGroupEgressArgs.builder()
                .cidrBlocks("0.0.0.0/0")
                .fromPort(0)
                .toPort(0)
                .protocol("all").build();

        new DefaultSecurityGroup("pulumi-sg",
                DefaultSecurityGroupArgs.builder()
                        .vpcId(vpc.id())
                        .ingress(tcpRule, selfIngressRule)
                        .egress(all)
                        .build());
    }

    private void createRouteTable(InternetGateway internetGateway) {
        var routeTable = new RouteTable("pulumi-rt",
                RouteTableArgs.builder()
                        .vpcId(vpc.id()).build());
        new Route("pulumi-route",
                RouteArgs.builder()
                        .routeTableId(routeTable.id())
                        .destinationCidrBlock("0.0.0.0/0")
                        .gatewayId(internetGateway.id()).build());
        subnetsId.applyValue(ids -> {
            var i = 1;
            for (var id : ids) {
                new RouteTableAssociation("pulumi-rt-association-" + i,
                        RouteTableAssociationArgs.builder()
                                .routeTableId(routeTable.id())
                                .subnetId(id).build());
                i++;
            }
            return null;
        });
    }

    private void createSubnets(Context ctx) {
        subnetRdsA = new Subnet("pulumi-subnet-rds-a", SubnetArgs.builder()
                .vpcId(vpc.id())
                .cidrBlock("10.0.0.0/24")
                .availabilityZone("eu-west-1a")
                .build());
        subnetRdsB = new Subnet("pulumi-subnet-rds-b", SubnetArgs.builder()
                .vpcId(vpc.id())
                .cidrBlock("10.0.2.0/24")
                .availabilityZone("eu-west-1b")
                .build());
        subnetEks = new Subnet("pulumi-subnet-eks", SubnetArgs.builder()
                .vpcId(vpc.id())
                .cidrBlock("10.0.1.0/24")
                .build());
        subnetEc2 = new Subnet("pulumi-subnet-ec2", SubnetArgs.builder()
                .vpcId(vpc.id())
                .cidrBlock("10.0.3.0/24")
                .build());
        subnetsId = Output.all(subnetEks.id(), subnetRdsB.id(), subnetRdsA.id(), subnetEc2.id());
        ctx.export("subnet_rds_a_id", subnetRdsA.id());
        ctx.export("subnet_rds_b_id", subnetRdsB.id());
        ctx.export("subnet_eks_id", subnetEks.id());
        ctx.export("subnet_ec2_id", subnetEc2.id());
    }

    public List<Subnet> subnetsRds() {
        return Arrays.asList(subnetRdsA, subnetRdsB);
    }

    public Output<List<String>> subnetsRdsId() {
        return Output.all(subnetRdsA.id(), subnetRdsB.id());
    }

    public Subnet subnetEc2() {
        return subnetEc2;
    }
}
