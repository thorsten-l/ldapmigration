package l9g.app.ldapmigration;

import com.unboundid.asn1.ASN1GeneralizedTime;
import com.unboundid.ldap.sdk.AddRequest;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.controls.RFC3672SubentriesRequestControl;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import jakarta.xml.bind.JAXB;
import java.io.FileWriter;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.net.ssl.SSLSocketFactory;
import l9g.app.ldapmigration.config.Configuration;
import l9g.app.ldapmigration.config.Credentials;
import l9g.app.ldapmigration.config.LdapEntry;
import l9g.app.ldapmigration.config.LdapHost;
import l9g.app.ldapmigration.config.MatchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
public class Ldapmigration1
{
  private final static Logger LOGGER = LoggerFactory.getLogger(Ldapmigration1.class.getName());

  private static long entryCounter = 0;

  private static void migrateLdap(String baseDn) throws Throwable
  {

    SearchRequest searchRequest = new SearchRequest(baseDn,
      SearchScope.ONE, "objectClass=ldapSubEntry");

    searchRequest.addControl(new RFC3672SubentriesRequestControl(true));

    SearchResult searchResult = sourceConnection.search(searchRequest);
    if (searchResult.getResultCode() == ResultCode.SUCCESS)
    {
      for (SearchResultEntry entry : searchResult.getSearchEntries())
      {
        AddRequest addRequest = new AddRequest(entry);
        addRequest.addControls(new RFC3672SubentriesRequestControl(true));
        /*
        addRequest.addControls(new ManageDsaITRequestControl(),
          new RFC3672SubentriesRequestControl(true),
          new SubentriesRequestControl());
         */

        System.out.println(destinationConnection.getConnectionName() + " / "
          + entry.getDN());

        try
        {
          destinationConnection.add(addRequest);
          System.out.println("  - success");
        }
        catch (LDAPException e)
        {
          System.out.println("  - failed : " + e.getMessage());
        }
      }
    }
    else
    {
      System.err.println(searchResult.getDiagnosticMessage());
      System.exit(0);
    }

    System.exit(0);

    searchRequest = new SearchRequest(baseDn, SearchScope.ONE,
      "objectClass=*");

    searchResult = sourceConnection.search(searchRequest);

    if (searchResult.getResultCode() == ResultCode.SUCCESS)
    {
      for (SearchResultEntry entry : searchResult.getSearchEntries())
      {
        System.out.println(++entryCounter + " : " + entry.getDN());

        if (entry.getDN().startsWith("o=ostfalia.de,")
          || entry.getDN().startsWith("o=hbk-bs.de,")
          || entry.getDN().startsWith("o=versus-wf.de,")
          || entry.getDN().startsWith("o=3landesmuseen.de,")
          || entry.getDN().startsWith("ou=archive,"))
        {
          System.out.println("*** skip entry " + entry.getDN());
        }
        else
        {
          LDAPResult addEntryResult = null;

          try
          {
            addEntryResult = destinationConnection.add(entry);
          }
          catch (LDAPException e)
          {
            if (e.getResultCode() != ResultCode.ENTRY_ALREADY_EXISTS)
            {
              System.out.println(e.getDiagnosticMessage());
              System.exit(0);
            }
          }

          if ((entryCounter % 1000) == 0)
          {
            System.out.println("####### Reconnect to destination LDAP.");
            destinationConnection.close();
            destinationConnection = getConnection(
              false, "localhost", 3389, "cn=Directory Manager",
              "gFWoFvnpoS9n857JaIOOUM2WYe97dSARrgnQT.Bi2cZcgN3SviVdHZdIRXNWi5ptZ");
          }

          if (addEntryResult != null
            && addEntryResult.getResultCode() != ResultCode.SUCCESS)
          {
            System.err.println(addEntryResult.getDiagnosticMessage());
            System.exit(0);
          }

          migrateLdap(entry.getDN());
        }
      }
    }
    else
    {
      System.err.println(searchResult.getDiagnosticMessage());
      System.exit(0);
    }

  }

  static LDAPConnection sourceConnection;

  static LDAPConnection destinationConnection;

  private static void migrateACIs(String baseDn) throws Throwable
  {
    System.out.println("migrateACIs");

    SearchRequest searchRequest = new SearchRequest(baseDn,
      SearchScope.BASE, "objectClass=*", "aci");

    SearchResult searchResult = sourceConnection.search(searchRequest);

    System.out.println(searchResult.getEntryCount());

    if (searchResult.getEntryCount() == 1)
    {
      SearchResultEntry entry = searchResult.getSearchEntries().get(0);
      Attribute acis = entry.getAttribute("aci");

      ArrayList<String> valueList = new ArrayList<>();

      for (String value : acis.getValues())
      {
        value = value.trim();
        valueList.add(value);

        Modification modification = new Modification(ModificationType.REPLACE,
          "aci", valueList.toArray(new String[0]));

        ModifyRequest modifyRequest = new ModifyRequest(entry.getDN(),
          modification);

        LDAPResult ldapResult = null;

        try
        {
          ldapResult = destinationConnection.modify(modifyRequest);
          if (ldapResult.getResultCode() != ResultCode.SUCCESS)
          {
            System.out.println("FR>  " + value);
            System.out.println(ldapResult);
          }
          else
          {
            System.out.println("S> " + value);
          }
        }
        catch (LDAPException e)
        {
          System.out.println("FE>  " + value);
          System.out.println("   - " + e.getExceptionMessage());
          valueList.remove(value);
        }
      }
    }
  }

  private static void migrateNsRoles(String baseDn) throws Throwable
  {
    SearchRequest searchRequest = new SearchRequest(
      baseDn, SearchScope.SUB,
      "&(objectClass=ldapSubEntry)(objectClass=nsRoleDefinition)", "*",
      "nsRoleDn");
    searchRequest.addControl(new RFC3672SubentriesRequestControl(true));
    SearchResult searchResult = sourceConnection.search(searchRequest);
    System.out.println(searchResult.getEntryCount() + " entries found.");

    if (searchResult.getResultCode() == ResultCode.SUCCESS)
    {
      for (SearchResultEntry entry : searchResult.getSearchEntries())
      {
        System.out.println(++entryCounter + " : " + entry.getDN());

        LDAPResult addEntryResult = null;

        try
        {
          addEntryResult = destinationConnection.add(entry);
        }
        catch (LDAPException e)
        {
          if (e.getResultCode() != ResultCode.ENTRY_ALREADY_EXISTS)
          {
            System.out.println(e.getDiagnosticMessage());
            System.exit(0);
          }
          else
          {
            System.out.println("  - already exists");
          }
        }

        if (addEntryResult != null
          && addEntryResult.getResultCode() != ResultCode.SUCCESS)
        {
          System.err.println(addEntryResult.getDiagnosticMessage());
          System.exit(0);
        }
      }
    }
    else
    {
      System.err.println(searchResult.getDiagnosticMessage());
      System.exit(0);
    }
  }

  private static void migrateNsRoleDNs(String baseDn) throws Throwable
  {
    SearchRequest searchRequest = new SearchRequest(
      baseDn, SearchScope.SUB, "nsRoleDn=*", "nsRoleDn");
    searchRequest.addControl(new RFC3672SubentriesRequestControl(true));
    SearchResult searchResult = sourceConnection.search(searchRequest);
    System.out.println(searchResult.getEntryCount() + " entries found.");

    if (searchResult.getResultCode() == ResultCode.SUCCESS)
    {
      for (SearchResultEntry entry : searchResult.getSearchEntries())
      {
        System.out.println(++entryCounter + " : " + entry.getDN());

        try
        {
          Modification modification = new Modification(ModificationType.REPLACE,
            "nsRoleDn", entry.getAttribute("nsRoleDn").getValues());

          ModifyRequest modifyRequest = new ModifyRequest(entry.getDN(),
            modification);

          LDAPResult ldapResult = destinationConnection.modify(modifyRequest);
        }
        catch (LDAPException e)
        {
          System.out.println("  - " + e.getExceptionMessage());
        }
      }
    }
    else
    {
      System.err.println(searchResult.getDiagnosticMessage());
      System.exit(0);
    }
  }

  private static ASN1GeneralizedTime getSoniaSyncTimestamp(String timestampDn)
    throws
    LDAPException
  {
    SearchResultEntry entry = destinationConnection.getEntry(
      timestampDn, "soniaSyncTimestamp");
    ASN1GeneralizedTime timestamp = new ASN1GeneralizedTime(entry.
      getAttributeValueAsDate("soniaSyncTimestamp"));
    System.out.println("timestamp dn: " + timestampDn
      + " (" + timestamp + ")");
    return timestamp;
  }

  private static void touchSoniaSyncTimestamp(String timestampDn,
    ASN1GeneralizedTime timestamp) throws
    LDAPException
  {

    Modification modification = new Modification(ModificationType.REPLACE,
      "soniaSyncTimestamp", timestamp.getStringRepresentation());

    ModifyRequest modifyRequest = new ModifyRequest(timestampDn,
      modification);

    LDAPResult ldapResult = destinationConnection.modify(modifyRequest);

    System.out.println("timestamp dn: " + timestampDn
      + " (" + timestamp.getStringRepresentation()
      + ") " + ldapResult.getResultCode());
  }

  public static void updateLdap(String baseDn) throws LDAPException
  {
    ASN1GeneralizedTime soniaSyncTimestamp = new ASN1GeneralizedTime();

    // ---------------
    SearchRequest searchRequest = new SearchRequest(baseDn, SearchScope.SUB,
      "|(objectClass=*)(objectClass=ldapSubEntry)", "dn");

    System.out.println("Start source search.");
    double startTimestamp = System.currentTimeMillis();
    SearchResult sourceSearchResult = sourceConnection.search(searchRequest);
    double endTimestamp = System.currentTimeMillis();
    System.out.println("done time=" + (endTimestamp - startTimestamp) / 1000.0);
    startTimestamp = endTimestamp;
    System.out.println("Start destination search.");
    SearchResult destinationSearchResult = destinationConnection.search(
      searchRequest);
    endTimestamp = System.currentTimeMillis();
    System.out.println("done time=" + (endTimestamp - startTimestamp) / 1000.0);

    int sourceEntries = sourceSearchResult.getEntryCount();
    int destinationEntries = destinationSearchResult.getEntryCount();

    System.out.println("build hashset from source DNs");

    HashSet<String> sourceDnSet = new HashSet<>();
    ArrayList<String> sourceDnList = new ArrayList<>();
    for (Entry entry : sourceSearchResult.getSearchEntries())
    {
      sourceDnSet.add(DN.normalize(entry.getDN()).toLowerCase());
      sourceDnList.add(DN.normalize(entry.getDN()).toLowerCase());
    }

    HashSet<String> destinationDnSet = new HashSet<>();
    ArrayList<String> destinationDnList = new ArrayList<>();
    for (Entry entry : destinationSearchResult.getSearchEntries())
    {
      destinationDnSet.add(DN.normalize(entry.getDN()).toLowerCase());
      destinationDnList.add(DN.normalize(entry.getDN()).toLowerCase());
    }

    System.out.println("# entries in source ldap = " + sourceEntries);
    System.out.println("# entries in destination ldap = " + destinationEntries);

    startTimestamp = System.currentTimeMillis();

    if (destinationSearchResult.getResultCode() == ResultCode.SUCCESS)
    {
      ArrayList<String> deleteDNs = new ArrayList<>();

      for (String destinationDn : destinationDnList)
      {
        if (destinationDn.equalsIgnoreCase("cn=ldapmigration,dc=sonia,dc=de")
          || destinationDn.equalsIgnoreCase("cn=389_ds_system,dc=sonia,dc=de"))
        {
          System.out.println("  -- SKIP " + destinationDn);
        }
        else
        {
          if (!sourceDnSet.contains(destinationDn))
          {
            System.out.println("  -- DELETE " + destinationDn);
            deleteDNs.add(destinationDn);
          }
        }
      }

      System.out.println("# entries to delete = " + deleteDNs.size());
      int deleteCounter = 0;

      for (int i = deleteDNs.size() - 1; i >= 0; i--)
      {
        String deleteDn = deleteDNs.get(i);
        System.out.println(++deleteCounter + "/" + deleteDNs.size()
          + " -> DELETE " + deleteDn);
        LDAPResult deleteResult = destinationConnection.delete(deleteDn);
        if (deleteResult.getResultCode() != ResultCode.SUCCESS)
        {
          System.out.println("*** DELETE Failed");
          System.exit(0);
        }
      }
    }

    endTimestamp = System.currentTimeMillis();
    System.out.println("done time=" + (endTimestamp - startTimestamp) / 1000.0);

    System.out.
      println("--- Add missing entries --------------------------------------");

    int addCounter = 0;

    for (String sourceDn : sourceDnList)
    {
      if (!destinationDnSet.contains(sourceDn)
        && !"cn=ocucs admin policy,dc=sonia,dc=de".equals(sourceDn))
      {
        System.out.println(++addCounter + " ADDING " + sourceDn);
        LDAPResult addResult = destinationConnection.add(
          sourceConnection.getEntry(sourceDn, "*", "nsRoleDn"));
        if (addResult.getResultCode() != ResultCode.SUCCESS)
        {
          System.out.println("ADD Failed");
          System.exit(0);
        }
      }
    }

    // ---------------
    ASN1GeneralizedTime lastSyncTimestamp = getSoniaSyncTimestamp(
      "cn=ldapmigration,dc=sonia,dc=de");

    // System.exit(0);
    searchRequest = new SearchRequest(baseDn, SearchScope.SUB,
      "(&(|(objectClass=*)(objectClass=ldapSubEntry))(modifyTimestamp>="
      + lastSyncTimestamp.getStringRepresentation() + "))", "*", "nsRoleDn");

    SearchResult searchResult = sourceConnection.search(searchRequest);
    System.out.println("# entries to update = " + searchResult.getEntryCount());
    int counter = 0;

    if (searchResult.getResultCode() == ResultCode.SUCCESS)
    {
      for (SearchResultEntry entry : searchResult.getSearchEntries())
      {
        System.out.println(++counter + "  dn: " + entry.getDN());

        if (DN.normalize(entry.getDN()).equals(DN.normalize(baseDn)))
        {
          System.out.println("  --- SKIP");
        }
        else
        {
          SearchResultEntry destinationEntry = destinationConnection.getEntry(
            entry.getDN(), "*", "nsRoleDn");

          if (destinationEntry != null)
          {
            List<Modification> modifications = Entry.diff(destinationEntry,
              entry, true);

            boolean nsRoleDnAddFound = false;

            for (Modification modification : modifications)
            {
              System.out.println(modification);
              if (modification.getAttributeName().equalsIgnoreCase("nsRoleDn")
                && modification.getModificationType() == ModificationType.ADD)
              {
                nsRoleDnAddFound = true;
              }
            }

            // modify
            if (modifications != null && modifications.size() > 0)
            {
              LDAPResult modifyResult = destinationConnection.modify(
                destinationEntry.getDN(), modifications);
              if (modifyResult.getResultCode() != ResultCode.SUCCESS)
              {
                System.out.println("  *** MODIFY FAILED");
                System.exit(0);
              }
            }
            else
            {
              System.out.println("  --- No modification detected");
            }
          }
        }
      }
    }

    touchSoniaSyncTimestamp("cn=ldapmigration,dc=sonia,dc=de",
      soniaSyncTimestamp);
  }

  private static LDAPConnection getConnection(boolean sslEnabled, String host,
    int port, String bindDn, String password) throws Exception
  {
    LDAPConnection ldapConnection;

    LDAPConnectionOptions options = new LDAPConnectionOptions();
    if (sslEnabled)
    {
      ldapConnection = new LDAPConnection(createSSLSocketFactory(), options,
        host, port, bindDn, password);
    }
    else
    {
      ldapConnection = new LDAPConnection(options, host, port, bindDn, password);
    }
    ldapConnection.setConnectionName(host);
    return ldapConnection;
  }

  private static SSLSocketFactory createSSLSocketFactory() throws
    GeneralSecurityException
  {
    SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
    return sslUtil.createSSLSocketFactory();
  }

  public static void main(String[] args) throws Throwable
  {
    LOGGER.info("{}", BuildProperties.getInstance());

    Configuration config = new Configuration();
    config.setBaseDn("dc=sonia,dc=de");
    config.setTimezone("Europe/Berlin");
    config.setSyncTimestampEntry(new LdapEntry(
      MatchType.equals, "cn=ldapmigration,dc=sonia,dc=de", "soniaSyncTimestamp"));
    config.setSourceHost(new LdapHost("dps1.sonia.de", 636, true,
      new Credentials("cn=Directory Manager", "3x2GtMZk")));
    config.setDestinationHost(new LdapHost("localhost", 3389, false,
      new Credentials("cn=Directory Manager",
        "gFWoFvnpoS9n857JaIOOUM2WYe97dSARrgnQT.Bi2cZcgN3SviVdHZdIRXNWi5ptZ")));

    ArrayList<LdapEntry> sourceIgnoreList = new ArrayList<>();
    sourceIgnoreList.add(new LdapEntry(MatchType.equals,
      "cn=ocucs admin policy,dc=sonia,dc=de", null));

    ArrayList<LdapEntry> destinationIgnoreList = new ArrayList<>();
    destinationIgnoreList.add(new LdapEntry(MatchType.equals,
      "cn=ldapmigration,dc=sonia,dc=de", null));
    destinationIgnoreList.add(new LdapEntry(MatchType.equals,
      "cn=389_ds_system,dc=sonia,dc=de", null));

    config.setSourceIgnoreEntries(sourceIgnoreList);
    config.setDestinationIgnoreEntries(destinationIgnoreList);

    JAXB.marshal(config, new FileWriter("config.xml"));

    System.exit(0);

    sourceConnection = getConnection(
      true, "dps1.sonia.de", 636, "cn=Directory Manager", "3x2GtMZk");

    destinationConnection = getConnection(
      false, "localhost", 3389, "cn=Directory Manager",
      "gFWoFvnpoS9n857JaIOOUM2WYe97dSARrgnQT.Bi2cZcgN3SviVdHZdIRXNWi5ptZ");

    // getSoniaSyncTimestamp("cn=ldapmigration,dc=sonia,dc=de");
    // touchSoniaSyncTimestamp("cn=ldapmigration,dc=sonia,dc=de");
    // getSoniaSyncTimestamp("cn=ldapmigration,dc=sonia,dc=de");
    // migrateLdap("dc=sonia,dc=de");
    updateLdap("dc=sonia,dc=de");
    // migrateACIs("dc=sonia,dc=de");
    // migrateNsRoles("dc=sonia,dc=de");
    // migrateNsRoleDNs("dc=sonia,dc=de");
  }

}
