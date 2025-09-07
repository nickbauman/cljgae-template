# {{name}}

{{name}} long description TODO

## Prerequisites

You will need [TODO][1]

[1]: https://github.com/TODO/TODO

## One-time quickstart instructions

*NOTE:* this is meant to be a "quickstart" to get an app engine app up and running. It doesn't use best practices 
regarding secrets handling and terraform variable substitution, which is beyond the scope of this tooling.

There's a basic terraform script that will create the app engine project and enable app engine in it.

From directory `quickstart`, execute, in succession, allowing each step to complete, the following terraform operations:

```text
terraform init
````

```text
terraform plan -out create_project_enable_appengine.plan.zip
```

```text
terraform apply "create_project_enable_appengine.plan.zip"
````

## Building

To build a deployable WAR:

```shell
lein ring uberwar
```

This requires that the App Engine Java SDK is installed locally and in your path.

## Automated testing

Through leiningen

```shell
lein test
```

## Running Locally

### Using the development script:

```shell
./run-dev.sh
```

This will:
1. Build your application as an executable JAR
2. Start the local App Engine development server
3. Make your app available at http://localhost:8080
4. Provide an admin console at http://localhost:8000

### Manual local development:

Alternatively, you can run the development server manually:

```shell
# Build the application
lein ring uberjar

# Run with gcloud
gcloud app run app.yaml --host=localhost --port=8080
```

### Requirements:
- [Google Cloud CLI](https://cloud.google.com/sdk/docs/install) with App Engine components
- Java {{java-runtime}} or later

## Deploying

To deploy to App Engine:

### Prerequisites
1. Make sure you have the [Google Cloud CLI](https://cloud.google.com/sdk/docs/install) installed
2. Authenticate with Google Cloud:
   ```shell
   gcloud auth login
   ```
3. Set your project ID:
   ```shell
   gcloud config set project {{name}}
   ```
4. Ensure App Engine is enabled for your project (the quickstart terraform script does this automatically)

### Deploy your application

Build and deploy using the modern approach:

```shell
# Build the application
lein ring uberjar

# Deploy to App Engine
gcloud app deploy app.yaml
```

Or use the provided deployment script:

```shell
./deploy.sh
```

### View your deployed application

```shell
gcloud app browse
```

This will open your deployed application in your default web browser.

### View logs

```shell
gcloud app logs tail -s default
```

## License

Copyright © {{year}}
