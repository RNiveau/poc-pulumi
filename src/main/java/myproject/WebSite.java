package myproject;

import com.pulumi.Context;
import com.pulumi.asset.FileAsset;
import com.pulumi.aws.s3.*;
import com.pulumi.aws.s3.inputs.BucketOwnershipControlsRuleArgs;
import com.pulumi.aws.s3.inputs.BucketWebsiteArgs;
import com.pulumi.resources.CustomResourceOptions;

public class WebSite {

    public static void createWebSite(Context ctx) {
        var bucket = new Bucket("my-bucket", BucketArgs.builder()
                .website(BucketWebsiteArgs.builder()
                        .indexDocument("index.html").build())
                .build());

        new BucketOwnershipControls("ownershipControls", BucketOwnershipControlsArgs.builder()
                .bucket(bucket.id())
                .rule(BucketOwnershipControlsRuleArgs.builder()
                        .objectOwnership("ObjectWriter")
                        .build())
                .build());

        var publicAccessBlock = new BucketPublicAccessBlock("publicAccessBlock", BucketPublicAccessBlockArgs.builder()
                .bucket(bucket.id())
                .blockPublicAcls(false)
                .build());

        new BucketObject("index.html", BucketObjectArgs.builder()
                .bucket(bucket.id()).source(new FileAsset("./index.html"))
                .contentType("text/html")
                .acl("public-read")
                .build(), CustomResourceOptions.builder()
                .dependsOn(publicAccessBlock)
                .build());

        ctx.export("bucketName", bucket.bucket());
        ctx.export("bucketEndpoint", bucket.websiteEndpoint().applyValue(websiteEndpoint -> String.format("http://%s", websiteEndpoint)));

    }

}
