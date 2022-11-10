
echo INFO: IMPORTANT: Set cn=Directory Manager password to "Y7EJm26Cl9jeMf.4CJl34NX3nTEn9ppge.uwqWJh5ecM6F737Q5W3iT82K1CZhVXc"

docker run --name 389ds-base --rm -p 3389:3389 -p 3636:3636 \
  -v `pwd`/data:/data 389ds:base-fedora
