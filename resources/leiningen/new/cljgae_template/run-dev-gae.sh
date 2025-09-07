#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

echo "Starting {{name}} with App Engine emulation..."
echo "Building application..."
lein clean
lein ring uberjar

echo "Starting App Engine development server..."
echo "App will be available at: http://localhost:8080"
echo "Admin console at: http://localhost:8000"
echo "Press Ctrl+C to stop the server"
echo ""

# Run using gcloud dev_appserver with App Engine service emulation
gcloud app run app.yaml --host=localhost --port=8080 --admin_host=localhost --admin_port=8000
