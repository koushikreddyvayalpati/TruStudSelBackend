# AWS Credentials Security Guide

## IMPORTANT: AWS Credentials Leak Prevention

This project recently had AWS credentials inadvertently committed to the repository. This is a serious security risk that could lead to unauthorized access to your AWS resources and potential financial and data loss.

## Security Best Practices

### 1. Never commit credentials to Git

- AWS access and secret keys should never be directly included in your application.properties or any other file that is committed to version control
- Use environment variables instead
- Add credential files to .gitignore

### 2. Environment Variables Setup

For local development:

```bash
# Set these in your shell profile (.bashrc, .zshrc, etc.)
export AWS_ACCESS_KEY=your_aws_access_key
export AWS_SECRET_KEY=your_aws_secret_key
```

For production environments:
- Set environment variables in your deployment platform (AWS, Heroku, etc.)
- Use AWS IAM roles where possible instead of access keys

### 3. Rotate Compromised Credentials

**IMMEDIATELY** after discovering credentials have been committed to a repository:

1. Deactivate the compromised AWS credentials in AWS IAM console
2. Create new credentials if needed
3. Update all systems and environment variables with the new credentials
4. Verify no unauthorized activity occurred during the compromise period

### 4. Using .gitignore

Our .gitignore file includes patterns to prevent accidentally committing credential files:

```
### AWS Credentials ###
src/main/resources/application.properties
**/application.properties
**/aws.properties
**/credentials
**/credentials.json
**/.aws/
```

### 5. Using the application.properties.sample File

- Use the provided sample file as a template
- Copy it to application.properties for local development
- Replace placeholders with environment variable references, not actual credentials

## For New Developers

1. Never add actual credentials to the codebase
2. Set up environment variables as described above
3. Use the sample files as templates
4. If you see credentials in code, report it immediately

## Questions?

If you have questions about secure credential handling, please contact the project lead. 