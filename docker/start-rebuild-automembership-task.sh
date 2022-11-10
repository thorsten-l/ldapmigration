ldapadd -h localhost -p 3389 -D "cn=Directory Manager" \
  -w <manager password> \
  -f start-rebuild-automembership-task.ldif
