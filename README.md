# TruStudSel Backend

Spring Boot application for TruStudSel with AWS DynamoDB and S3 integration.

## Environment Setup

This project requires AWS credentials to run. For security reasons, these credentials are not stored in the repository.

### Setting Environment Variables

Set the following environment variables to run the application:

```bash
# AWS Credentials
export AWS_ACCESS_KEY=your-aws-access-key
export AWS_SECRET_KEY=your-aws-secret-key
```

### Alternative: Using application-local.properties

You can also create a local properties file that won't be committed to the repository:

1. Create a file named `application-local.properties` in the `src/main/resources` directory
2. Add your AWS credentials:

```properties
aws.s3.access-key=your-aws-access-key
aws.s3.secret-key=your-aws-secret-key
aws.dynamodb.access-key=your-aws-access-key
aws.dynamodb.secret-key=your-aws-secret-key
```

3. Run the application with the local profile:

```bash
./mvnw spring-boot:run -Dspring.profiles.active=local
```

## Building and Running

```bash
./mvnw clean install
./mvnw spring-boot:run
``` 