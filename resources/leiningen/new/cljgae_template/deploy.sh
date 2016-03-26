#!/bin/bash

DEPLOY_DIR="target/deploy"
APP_VERSION=0.1.0-SNAPSHOT

if [ -d $DEPLOY_DIR ] 
    then
        rm -r $DEPLOY_DIR
fi

lein clean
lein ring uberwar

unzip -d $DEPLOY_DIR target/{{name}}-$APP_VERSION-standalone.war 

appcfg.sh --oauth2 update $DEPLOY_DIR
