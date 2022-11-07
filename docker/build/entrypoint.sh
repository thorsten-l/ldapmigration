#!/bin/bash
if [ ! -d "/data/etc" ]; then
  echo "Initialize /data/etc" 
  cp -r /etc/dirsrv.dist/* /data
fi
exec /usr/libexec/dirsrv/dscontainer -r
