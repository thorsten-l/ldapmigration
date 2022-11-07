#!/bin/bash

BUILD_VERSION=2.2.3
echo "BUILD_VERSION=$BUILD_VERSION"
docker build --build-arg BUILD_VERSION="$BUILD_VERSION" \
  -t "389ds:$BUILD_VERSION-fedora-test" -f Dockerfile.2_2_3 .
