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

7. start 389ds `docker compose up -d`
8. `docker exec -it 389ds dsconf slapd-localhost backend create --suffix="dc=sonia,dc=de" --be-name="sonia"`
9. `docker exec -it 389ds dsidm -b "dc=sonia,dc=de" slapd-localhost initialise`

10. edit `config.xml`

11. `./ldapmigrate.jar -i`

12. done!

## Successfully Tested
- version `tludewig/389ds:2.2.3-fedora`
- version `tludewig/389ds:2.0.16-fedora`

