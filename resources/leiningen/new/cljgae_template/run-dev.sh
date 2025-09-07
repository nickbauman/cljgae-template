#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

echo "Starting {{name}} in development mode..."
echo "Building application..."
lein clean
lein ring uberjar

echo "Setting up development environment..."
export PORT=8080
export GOOGLE_CLOUD_PROJECT={{name}}
export GAE_ENV=development

echo "Application will be available at: http://localhost:8080"
echo "Press Ctrl+C to stop the server"
echo ""

# Run the JAR directly for fast development
java -Dfile.encoding=UTF-8 -jar target/{{name}}-*-standalone.jar
