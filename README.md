# LDAP Migration to ds389

## Initialization

> Steps to initialize and start the ldap migration.

### 1. Start 389ds

```bash
cd docker
docker compose up -d
```

### 2. Copy Directory Manager Password from LOG. you need it later for ldapmigration config file

```bash
docker compose logs -f
```

> Look for following message, containing the generated password:

```bash
...
INFO: IMPORTANT: Set cn=Directory Manager password to "xyz. a very long password"`
...
```

### 3. Edit `config.xml` (see file config.sample)

> Insert copied Password into `<password></password>` of the destinationHost credentials configuration.
> Fill out the settings for the hosts according to your needs.

### 4. Wait until log message

   `INFO: 389-ds-container started.`

### 5. Initial Settings

```bash
docker exec 389ds dsconf localhost config replace nsslapd-maxbersize=4194304
docker exec 389ds dsconf localhost config replace nsslapd-maxsasliosize=4194304
docker exec 389ds dsconf localhost config replace nsslapd-allow-anonymous-access=off
docker exec 389ds dsconf localhost config replace nsslapd-dynamic-plugins=on
docker exec 389ds dsconf localhost plugin memberof enable

docker exec 389ds dsconf localhost config get

#
# optional: disable syntax check if syntax errors on source are not correctable
#
docker exec 389ds dsconf localhost config replace nsslapd-syntaxcheck=off

#
# optional: change Directory Manager password
#
docker exec 389ds dsconf localhost config replace nsslapd-rootpw=<new password>

#
# optional: import existing server certificate
#
cp server.key server.crt ./data
docker exec 389ds dsctl localhost tls import-server-key-cert /data/server.crt /data/server.key
docker exec 389ds dsconf localhost security certificate list
rm ./data/server.key ./data/server.crt

#
# optional: change ports to standard 389/636
#
docker exec 389ds dsconf localhost config replace nsslapd-port=389
docker exec 389ds dsconf localhost config replace nsslapd-securePort=636
```

### 6. Stop 389ds

```bash
docker compose stop
```

### 7. Add additional schema files required for migration

```bash
cp ../schema/* ./data/config/schema
```

### 8. Start 389ds

```bash
docker compose start
```

### 9. Configure uniqueness plugin for some attributes. here uid, mail and mailAlternateAddress

```bash
docker exec 389ds dsconf localhost plugin attr-uniq set --enabled on --attr-name uid mail mailAlternateAddress --subtree "dc=sonia,dc=de" --across-all-subtrees on "attribute uniqueness"
docker exec 389ds dsconf localhost plugin attr-uniq show "attribute uniqueness"
```

### 10. Create and initialize suffix

```bash
docker exec 389ds dsconf slapd-localhost backend create --suffix="dc=sonia,dc=de" --be-name="sonia"
docker exec 389ds dsidm -b "dc=sonia,dc=de" slapd-localhost initialise
```

### 11. Build ldapmigration tool and import data from source LDAP

```bash
cd ..
mvn clean package
cp -p target/ldapmigration.jar .
./ldapmigration.jar -i
```

### 12. Update and add indexes to improve performance

```bash
# update indexes
docker exec 389ds dsconf localhost backend index set --attr givenName --add-type approx sonia
docker exec 389ds dsconf localhost backend index set --attr mail --add-type approx sonia
docker exec 389ds dsconf localhost backend index set --attr mailAlternateAddress --add-type pres --add-type sub --add-type approx sonia
docker exec 389ds dsconf localhost backend index set --attr uid --add-type pres --add-type sub sonia

# add indexes
docker exec 389ds dsconf localhost backend index add --attr departmentNumber --index-type eq --index-type pres sonia
docker exec 389ds dsconf localhost backend index add --attr displayName --index-type eq --index-type pres sonia
docker exec 389ds dsconf localhost backend index add --attr employeeNumber --index-type eq --index-type pres sonia
docker exec 389ds dsconf localhost backend index add --attr employeetype --index-type eq --index-type pres sonia
docker exec 389ds dsconf localhost backend index add --attr facsimileTelephoneNumber --index-type eq --index-type pres --index-type sub sonia
docker exec 389ds dsconf localhost backend index add --attr gecos --index-type eq --index-type pres --index-type sub sonia
docker exec 389ds dsconf localhost backend index add --attr gidnumber --index-type eq --index-type pres sonia
docker exec 389ds dsconf localhost backend index add --attr l --index-type eq --index-type pres sonia
docker exec 389ds dsconf localhost backend index add --attr mailDomainStatus --index-type eq --index-type pres sonia
docker exec 389ds dsconf localhost backend index add --attr mailEquivalentAddress --index-type eq --index-type pres --index-type sub --index-type approx sonia
docker exec 389ds dsconf localhost backend index add --attr mailUserStatus --index-type eq --index-type pres sonia
docker exec 389ds dsconf localhost backend index add --attr memberUid --index-type eq --index-type pres sonia
docker exec 389ds dsconf localhost backend index add --attr nsRoleDN --index-type eq --index-type pres sonia
docker exec 389ds dsconf localhost backend index add --attr o --index-type eq --index-type pres sonia
docker exec 389ds dsconf localhost backend index add --attr ou --index-type eq --index-type pres sonia
docker exec 389ds dsconf localhost backend index add --attr uidnumber --index-type eq --index-type pres sonia
...

# list indexes
docker exec 389ds dsconf localhost backend index list sonia

# reindex
docker exec 389ds dsconf localhost backend index reindex sonia

# observe reindexing progress in log
docker logs -f 389ds
```

### 13. Re-adjust performance parameters: see

> [Tuning the performance](https://access.redhat.com/documentation/en-us/red_hat_directory_server/12/html-single/tuning_the_performance_of_red_hat_directory_server),
> [Administration Guide](https://access.redhat.com/documentation/en-us/red_hat_directory_server/11/html-single/administration_guide/index)

```bash
#
# roughly estimated for an LDAP instance with 120000 entries.
#

# search size limit: specifies the maximum number of entries to return from a search operation
docker exec 389ds dsconf slapd-localhost config replace nsslapd-sizelimit=50000

# Index Scan Limit: specifies the maximum number of entry IDs loaded from an index file for search results, it can make a search treated as an unindexed.
docker exec 389ds dsconf slapd-localhost backend config set --idlistscanlimit=50000

# Lookthrough Limit: specifies how many entries are examined for a search operation
docker exec 389ds dsconf slapd-localhost backend config set --lookthroughlimit=50000
```

### 14. Configure and limit logging rotation

```bash
# configure error log to keep maximum 5 logs and to rotate log files with a 100 MB size or every 1 week, enter:
docker exec 389ds dsconf localhost config replace nsslapd-errorlog-maxlogsperdir=5 nsslapd-errorlog-maxlogsize=100 nsslapd-errorlog-logrotationtime=1 nsslapd-errorlog-logrotationtimeunit=week
docker exec 389ds dsconf slapd-localhost config get | grep -i errorlog

# configure access log to keep maximum 10 logs and to rotate log files with a 100 MB size or every 1 week, enter:
docker exec 389ds dsconf localhost config replace nsslapd-accesslog-maxlogsperdir=10 nsslapd-accesslog-maxlogsize=100 nsslapd-accesslog-logrotationtime=1 nsslapd-accesslog-logrotationtimeunit=week
docker exec 389ds dsconf slapd-localhost config get | grep -i accesslog
```

### __15. DONE__

___

## Successfully Tested on

- version `tludewig/389ds:2.2.5-fedora`
- version `tludewig/389ds:2.0.16-fedora`

___

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
