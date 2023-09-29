package myproject;

import com.pulumi.Context;
import com.pulumi.aws.ec2.*;

public class DebuggingHost {

    public static void createHost(Network network, Context ctx) {
        var keyPair = new KeyPair("pulumi-keypair", new KeyPairArgs.Builder()
                .publicKey("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDA5rnfxV4KznSxshZXIpWINtf5MU/ntPBKlOTtXHO+iHMYJIwT2mXVNTbhmPpRtNquxFNfoDpvqUK5DcRq1wCsqh2uVsOPrk1dHjLTY/I7WKhzfulTa6FKci9TZRh3onF1GRNiaG5ytgVKlO0oQOQy3qtMJrI/HudXnVqeiHZbDmatvKLnIcB66M4X9oi0ayoOAutDUVnlxhkEit/f8DxdUQZN5kiMwlLNDIAGBgkGWS+YlzxeTrexyZ4eUxyA3ewleo6yUk07GM1j/4sEQ1vflyk1+WjBipe07POOUQQvkO7Z4iXWTEhC6i4RINs0n/UddBhW+ItIpljIHiueEfkGXVRqTQsqjJ8pIXdiJ7bUTOAypL69W1bGzUSTiJBnzANupb3Djivgx0eJOvwo3eOQJr1vMhqOCKdhW/iCkI4fYe0cZ4Tpc8HpPLS1Txs9CdqeQFAq0FxsoBI3tYTc1IvNeLzjBoh/uXQEOWeqmChNEZFEiw65LLPP6m+i7KjVAb8HeBYkARwtR1fUJ651hjEIHbGrwP5RhmZNOdV3f8tJ1XLreK+EHKINEk0KHvMnn2isaHYp3ZKJESDo21BVLzwn/NO5k5i8UEz1ejKPCq07TZ86AAsyi2b8v2oJT7AU2WkcCXO4PkYjo0MNUr1nHZNWetzxU/sjA3cD0hV75ky7RQ==")
                .build());

        var ec2 = new Instance("debugging-host", InstanceArgs.builder()
                .availabilityZone(network.subnetEc2().availabilityZone().applyValue(x -> x))
                .instanceType("t3.micro")
                .subnetId(network.subnetEc2().id())
                .ami("ami-08f32efd140b7d89f")
                .associatePublicIpAddress(true)
                .keyName(keyPair.keyName())
                .build());

        ctx.export("debugging.publicIp", ec2.publicIp());
        ctx.export("debugging.publicDns", ec2.publicDns());
        ctx.export("debugging.sshKeyName", ec2.keyName());
    }

}
