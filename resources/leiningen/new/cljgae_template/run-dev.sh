#!/bin/bash

DEPLOY_DIR="target/deploy"
APP_VERSION=0.1.0-SNAPSHOT

if [ -d $DEPLOY_DIR ] 
    then
        rm -r $DEPLOY_DIR
fi

lein ring uberwar

unzip -d $DEPLOY_DIR target/{{name}}-$APP_VERSION-standalone.war 

dev_appserver.sh  --generated_dir=/tmp/dev_appserver  $DEPLOY_DIR
