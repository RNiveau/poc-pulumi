package myproject;

import com.pulumi.Context;
import com.pulumi.aws.rds.Instance;
import com.pulumi.aws.rds.InstanceArgs;
import com.pulumi.aws.rds.SubnetGroup;
import com.pulumi.aws.rds.SubnetGroupArgs;

public class RDS {

    public static void createRDS(String name, Network network, Context ctx) {
        var subnetGroup = new SubnetGroup("pulumi_rds_subnet_group", SubnetGroupArgs.builder()
                .subnetIds(network.subnetsRdsId()).build());

        var rds = new Instance(name, InstanceArgs.builder()
                .allocatedStorage(10)
                .dbName("test")
                .engine("mysql")
                .engineVersion("8.0")
                .instanceClass("db.t3.micro")
                .skipFinalSnapshot(true)
                .username("admin")
                .password(ctx.config().requireSecret("rds_password"))
                .dbSubnetGroupName(subnetGroup.name())
                .build());
        ctx.export("rds_id", rds.id());
    }
}
