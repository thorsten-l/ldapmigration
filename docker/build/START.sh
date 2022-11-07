
docker run --name 389ds-base --rm -p 3389:3389 -p 3636:3636 \
  -v `pwd`/data:/data 389ds:base-fedora-arm64
