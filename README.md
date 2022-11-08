# INIT

0. `cd docker`

1. start 389ds `docker compose up`
2. copy Directory Manager Password from LOG

    `INFO: IMPORTANT: Set cn=Directory Manager password to "xyz. a very long password"`

3. wait until log message:

   `INFO: 389-ds-container started.`

4. Initial Settings

```text
docker exec 389ds dsconf localhost config replace nsslapd-syntaxcheck=off
docker exec 389ds dsconf localhost config replace nsslapd-maxbersize=4194304
docker exec 389ds dsconf localhost config replace nsslapd-maxsasliosize=4194304
docker exec 389ds dsconf localhost config replace nsslapd-dynamic-plugins=on
docker exec 389ds dsconf localhost plugin memberof enable
```

5. stop 389ds `docker compose stop`
6. copy `../schema/` into `data/slapd-localhost/schema/`
7. start 389ds `docker compose up -d`
8. `docker exec -it 389ds dsconf slapd-localhost backend create --suffix="dc=sonia,dc=de" --be-name="sonia"`
9. `docker exec -it 389ds dsidm -b "dc=sonia,dc=de" slapd-localhost initialise`

10. edit `config.xml` - Directory Manager password...

11. `./ldapmigrate.jar -i`

12. done!

## Successfully Tested
- version `tludewig/389ds:2.2.3-fedora`
- version `tludewig/389ds:2.0.16-fedora`

## Settings

### nsslapd-ignore-virtual-attrs

#### ldapsearch

`ldapsearch -h localhost -p 3389 -b 'cn=config' -s base -D 'cn=Directory Manager' -w ******** nsslapd-ignore-virtual-attrs`

#### ldapmodify

`echo "dn: cn=config\nchangetype: modify\nreplace: nsslapd-ignore-virtual-attrs\nnsslapd-ignore-virtual-attrs: on" | ldapmodify -h localhost -p 3389 -D 'cn=Directory Manager' -w ********`

#### dsconf config get

`docker exec -it 389ds dsconf localhost config get nsslapd-ignore-virtual-attrs`

#### dsconf config replace

`docker exec 389ds dsconf localhost config replace nsslapd-ignore-virtual-attrs=on`
