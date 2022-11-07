# INIT

1. start 389ds `docker compose up`
2. copy Directory Manager Password from log
3. wait until log message:

   `INFO: 389-ds-container started.`

4. stop 389ds `docker compose stop`
5. copy `schema/` into `data/slapd-localhost/schema/`
6. edit `data/slapd-localhost/dse.ldif`

  - nsslapd-maxbersize: 4194304
  - nsslapd-maxsasliosize: 4194304
  - nsslapd-syntaxcheck: off
  - nsslapd-ignore-virtual-attrs: on
  - nsslapd-dynamic-plugins: on

7. start 389ds `docker compose up -d`
8. `docker exec -it 389ds dsconf slapd-localhost backend create --suffix="dc=sonia,dc=de" --be-name="sonia"`
9. `docker exec -it 389ds dsidm -b "dc=sonia,dc=de" slapd-localhost initialise`

10. edit `config.xml`

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
