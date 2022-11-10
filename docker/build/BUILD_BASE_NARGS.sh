#!/bin/bash

BUILD_VERSION=base
OS_VERSION=$1
DS_VERSION=$2
echo "OS_VERSION=$OS_VERSION"
echo "DS_VERSION=$DS_VERSION"
echo "BUILD_VERSION=$BUILD_VERSION"
docker build \
  --build-arg OS_VERSION="$OS_VERSION" \
  --build-arg DS_VERSION="$DS_VERSION" \
  -t "389ds:$BUILD_VERSION-fedora" -f Dockerfile.nargs .
