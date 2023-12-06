package myproject;

import com.pulumi.Context;
import com.pulumi.aws.eks.Addon;
import com.pulumi.aws.eks.AddonArgs;
import com.pulumi.aws.iam.*;
import com.pulumi.eks.Cluster;
import com.pulumi.eks.ClusterArgs;

public class EKS {

    public Cluster createEKS(Network network, Context ctx) {
        Role role = getClusterRole();
        Role nodeRole = getNodeRole();
        Policy eksAdminPolicy = createEksAdminPolicy();

        var cluster = new Cluster("pulumi-cluster", ClusterArgs.builder()
                .minSize(2)
                .maxSize(5)
                .desiredCapacity(2)
                .vpcId(network.vpc().id())
                .version("1.27")
                .instanceType("t2.small")
                .publicSubnetIds(network.subnetsEksId())
                .privateSubnetIds(network.subnetsEksId())
                .serviceRole(role)
                .build());


//        new NodeGroup("pulumi-ng-default", NodeGroupArgs.builder()
//                .clusterName(cluster.name())
//                .nodeGroupName("default")
//                .nodeRoleArn(nodeRole.arn())
//                .subnetIds(network.subnetsEksId())
//                .scalingConfig(NodeGroupScalingConfigArgs.builder()
//                        .desiredSize(2)
//                        .maxSize(5)
//                        .minSize(2)
//                        .build())
//                .build());

        managedAddOn(cluster);

        ctx.export("eks-id", cluster.eksCluster().applyValue(x -> x.id()));
        ctx.export("eks-role-id", role.id());
//        ctx.export("eks-node-role-id", nodeRole.id());
        ctx.export("eks-admin-policy-arn", eksAdminPolicy.arn());
        cluster.kubeconfigJson();
        return cluster;
    }

    private Policy createEksAdminPolicy() {
        var eksAdminPolicy = new Policy("pulumi-eks-admin-policy", new PolicyArgs.Builder()
                .policy(
                        """
                        {
                                "Version": "2012-10-17",
                                "Statement": [
                                    {
                                      "Action": "eks:*",
                                      "Resource": "*",
                                      "Effect": "Allow"
                                    }
                                  ]
                                }
                                """
                )
                .build());

        var group = new Group("pulumi-eks-group", new GroupArgs.Builder()
                .build());

        new GroupPolicyAttachment("pulumi-eks-group-attachment",
                new GroupPolicyAttachmentArgs.Builder()
                        .group(group.name())
                        .policyArn(eksAdminPolicy.arn())
                        .build());
        return eksAdminPolicy;
    }

    private Role getNodeRole() {
        var nodeRole = new Role("pulumi-eks-node-role", RoleArgs.builder()
                .assumeRolePolicy("""
                        {
                            "Version": "2012-10-17",
                            "Statement": [
                                {
                                    "Effect": "Allow",
                                    "Action": [
                                        "sts:AssumeRole"
                                    ],
                                    "Principal": {
                                        "Service": [
                                            "ec2.amazonaws.com"
                                        ]
                                    }
                                }
                            ]
                        }
                        """).build());

        new RolePolicyAttachment("pulumi-eks-policy-node-attachment-1",
                RolePolicyAttachmentArgs.builder()
                        .policyArn("arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly")
                        .role(nodeRole.id()).build());

        new RolePolicyAttachment("pulumi-eks-policy-node-attachment-2",
                RolePolicyAttachmentArgs.builder()
                        .policyArn("arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy")
                        .role(nodeRole.id()).build());
        return nodeRole;
    }

    private void managedAddOn(Cluster cluster) {
        new Addon("kube-proxy", AddonArgs.builder()
                .addonName("kube-proxy")
                .addonVersion("v1.27.1-eksbuild.1")
                .clusterName(cluster.eksCluster().apply(com.pulumi.aws.eks.Cluster::name))
                .resolveConflictsOnCreate("OVERWRITE")
                .resolveConflictsOnUpdate("OVERWRITE")
                .build());
        new Addon("vpc-cni", AddonArgs.builder()
                .addonName("vpc-cni")
                .addonVersion("v1.12.6-eksbuild.2")
                .clusterName(cluster.eksCluster().apply(com.pulumi.aws.eks.Cluster::name))
                .resolveConflictsOnCreate("OVERWRITE")
                .resolveConflictsOnUpdate("OVERWRITE")
                .build());
        new Addon("coredns", AddonArgs.builder()
                .addonName("coredns")
                .addonVersion("v1.10.1-eksbuild.1")
                .clusterName(cluster.eksCluster().apply(com.pulumi.aws.eks.Cluster::name))
                .resolveConflictsOnCreate("OVERWRITE")
                .resolveConflictsOnUpdate("OVERWRITE")
                .build());
    }

    private Role getClusterRole() {
        var role = new Role("pulumi-eks-role", RoleArgs.builder()
                .assumeRolePolicy(
                        """
                                {
                                    "Version": "2012-10-17",
                                    "Statement": [
                                        {
                                            "Effect": "Allow",
                                            "Principal": {
                                                "Service": "eks.amazonaws.com"
                                            },
                                            "Action": "sts:AssumeRole"
                                        }
                                    ]
                                }
                                """).build());

        new RolePolicyAttachment("pulumi-eks-policy-attachment",
                RolePolicyAttachmentArgs.builder()
                        .policyArn("arn:aws:iam::aws:policy/AmazonEKSClusterPolicy")
                        .role(role.id()).build());
        return role;
    }
}
