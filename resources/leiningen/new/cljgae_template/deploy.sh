#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail
set -o xtrace

# determine whether the project for the appengine app is even accessible:
# this will fail and stop the deploy right away if the project doesn't exist or is not accessible
gcloud projects describe {{name}}

DEPLOY_DIR="target"
APP_VERSION=0.1.0-SNAPSHOT

if [ -d $DEPLOY_DIR ]
    then
        rm -r $DEPLOY_DIR
fi

lein clean
lein ring uberwar

TARGET_DEPLOY=$DEPLOY_DIR/{{name}}-$APP_VERSION
mkdir $TARGET_DEPLOY
unzip -d $TARGET_DEPLOY target/{{name}}-$APP_VERSION-standalone.war

mvn package appengine:deployAll -e -X
