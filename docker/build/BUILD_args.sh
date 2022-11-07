#!/bin/bash

BUILD_VERSION=2.2.3
BUILD_SUB_VERSION=2.fc37

echo "BUILD_VERSION=$BUILD_VERSION"
echo "BUILD_SUB_VERSION=$BUILD_SUB_VERSION"

docker build \
  --build-arg DS_VERSION="$BUILD_VERSION" \
  --build-arg DS_SUBVERSION="$BUILD_SUB_VERSION" \
  -t "389ds:$BUILD_VERSION-fedora" \
  -f Dockerfile.args .
