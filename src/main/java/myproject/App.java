package myproject;

import com.pulumi.Pulumi;

public class App {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            var network = new Network().createNetwork(ctx);
//            DebuggingHost.createHost(network, ctx);
//            RDS.createRDS("test", network, ctx);
             new EKS().createEKS(network, ctx);
//            WebSite.createWebSite(ctx);
        });
    }
}
