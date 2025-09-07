#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail
set -o xtrace

# Verify project access and configuration
echo "Verifying project access..."
gcloud projects describe {{name}}

echo "Setting project context..."
gcloud config set project {{name}}

echo "Building application..."
lein clean
lein ring uberjar

echo "Deploying to App Engine..."
gcloud app deploy app.yaml --quiet

echo "Deployment complete!"
echo "View your app at: https://{{name}}.appspot.com"
echo "View logs with: gcloud app logs tail -s default"
