#!/bin/bash

if [ $# -ge 1 ]; then

OS_VERSION=$1
DS_VERSION=$2
echo "OS_VERSION=$OS_VERSION"
echo "DS_VERSION=$DS_VERSION"

docker buildx build --file Dockerfile.nargs \
  --build-arg OS_VERSION="$OS_VERSION" \
  --build-arg DS_VERSION="$DS_VERSION" \
  --push \
  --platform linux/arm64/v8,linux/amd64 \
  --tag "tludewig/389ds:$DS_VERSION-fedora" .

docker buildx build --file Dockerfile.nargs \
  --build-arg OS_VERSION="$OS_VERSION" \
  --build-arg DS_VERSION="$DS_VERSION" \
  --push \
  --platform linux/arm64/v8,linux/amd64 \
  --tag "tludewig/389ds:$DS_VERSION-fc$OS_VERSION" .

fi
