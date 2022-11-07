docker run --name 389ds-base --rm \
  -v `pwd`/data:/data -it 389ds:base-fedora-arm64 /bin/bash
