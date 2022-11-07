#!/bin/bash

BUILD_VERSION=2.2.3
echo "BUILD_VERSION=$BUILD_VERSION"
docker buildx build --file Dockerfile.2_2_3 --build-arg BUILD_VERSION="$BUILD_VERSION" \
--push \
--platform linux/arm64/v8,linux/amd64 \
--tag "tludewig/389ds:$BUILD_VERSION-fedora" .
