#!/bin/bash
if [ ! -d "/data/config" ]; then
  echo "Initialize /data" 
  cp -r /etc/dirsrv.dist/* /data
fi
exec /usr/libexec/dirsrv/dscontainer -r
