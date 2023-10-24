# {{name}}

{{name}} long description TODO

## Prerequisites

You will need [TODO][1]

[1]: https://github.com/TODO/TODO

## One-time setup instructions

From directory `project-setup` use terraform script to create the appengine project and enable appengine.

First create the project to house the app engine app (unless you already have one)

Then execute, in succession, allowing each step to complete, the following terraform operations

```shell
terraform init
````
```shell 
terraform plan -out create_project_enable_appengine.plan.zip
```
```shell 
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

## Running

To Run locally:

```shell
./run-dev.sh
```

This requires that the App Engine Java SDK is installed locally and in your path.

## Deploying

To deploy to App Engine, make sure the project ID exists, appenegine is enabled, its permissions configured, and have 
been initialized to the Java11 Runtime. If you use the "create project" terraform script to do this for you, you should 
be all set.

```shell
./deploy.sh
```

This requires that the App Engine Java SDK is installed locally and in your path.

## License

Copyright © {{year}}
