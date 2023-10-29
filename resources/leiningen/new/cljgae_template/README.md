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
