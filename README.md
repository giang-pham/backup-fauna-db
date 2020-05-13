# Fauna DB backup to s3 (Java)

Use the following instructions to deploy the sample application.

# Requirements
- [Java 8 runtime environment (SE JRE)](https://www.oracle.com/java/technologies/javase-downloads.html)
- [The AWS CLI v1](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html).

# Setup
To create a new bucket for deployment artifacts, run `1-create-bucket.sh`.

    s3-java$ ./1-create-bucket.sh
    make_bucket: lambda-fauna-a5e491dbb5b22e0d

# Deploy
To deploy the application, run `3-deploy.sh`.

This script uses AWS CloudFormation to deploy the Lambda functions and an IAM role. If the AWS CloudFormation stack that contains the resources already exists, the script updates it with any changes to the template or function code.

You can also build the application with Maven. To use maven, add `mvn` to the command.

    java-basic$ ./3-deploy.sh mvn
    [INFO] Scanning for projects...
    [INFO] -----------------------< com.example:s3-java >-----------------------
    [INFO] Building s3-java-function 1.0-SNAPSHOT
    [INFO] --------------------------------[ jar ]---------------------------------
    ...


# Cleanup
To delete the application, run `6-cleanup.sh`.

    $ ./4-cleanup.sh
