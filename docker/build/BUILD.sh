#!/bin/bash

BUILD_VERSION=base
echo "BUILD_VERSION=$BUILD_VERSION"
docker build --build-arg BUILD_VERSION="$BUILD_VERSION" -t "389ds:$BUILD_VERSION-fedora-arm64" -f Dockerfile .
