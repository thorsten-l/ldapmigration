#!/bin/bash
cd /data

if [ ! -d "/data/schema" ]; then
  echo "Initialize /data" 
  cp -r /etc/dirsrv.dist/* /data
elif [ ! -L "/data/config" ]; then
  echo "/data/config directory not symlink"
  cp /data/config/container.inf slapd-localhost
  rm -fr /data/config
  ln -s slapd-localhost config
fi

exec /usr/libexec/dirsrv/dscontainer -r
