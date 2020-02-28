#!/bin/bash

DEPLOY_DIR="target"
APP_VERSION=0.1.0-SNAPSHOT

if [ -d $DEPLOY_DIR ]
    then
        rm -r $DEPLOY_DIR
fi

lein ring uberwar

TARGET_DEPLOY=$DEPLOY_DIR/{{name}}-$APP_VERSION
mkdir $TARGET_DEPLOY
unzip -d $TARGET_DEPLOY target/{{name}}-$APP_VERSION-standalone.war

mvn appengine:run
