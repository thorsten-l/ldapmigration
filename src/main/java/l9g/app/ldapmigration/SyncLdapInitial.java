package l9g.app.ldapmigration;

import com.unboundid.asn1.ASN1GeneralizedTime;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import java.util.ArrayList;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
public class SyncLdapInitial
{
  private final static Logger LOGGER = LoggerFactory.getLogger(
    SyncLdapInitial.class.
      getName());

  public static void deleteAllDestinationEntries() throws Throwable
  {
    LOGGER.info("deleteAllDestinationEntries()");
    double startTimestamp = System.currentTimeMillis();

    String baseDn = Ldapmigration.getConfig().getBaseDn();

    final HashSet<String> destinationIgnoreDnSet = new HashSet<>();

    for (l9g.app.ldapmigration.config.LdapEntry entry
      : Ldapmigration.getConfig().getDestinationIgnoreEntries())
    {
      destinationIgnoreDnSet.add(DN.normalize(entry.getDn()).toLowerCase());
    }
    
    destinationIgnoreDnSet.add(baseDn);

    SearchRequest searchRequest = new SearchRequest(baseDn, SearchScope.SUB,
      "|(objectClass=*)(objectClass=ldapSubEntry)", "dn");

    LOGGER.info("Start destination search.");

    SearchResult destinationSearchResult = ConnectionHandler.
      getDestinationConnection().
      search(searchRequest);
    
    if (destinationSearchResult.getResultCode() == ResultCode.SUCCESS)
    {
      ArrayList<String> deleteDNs = new ArrayList<>();

      for ( Entry entry : destinationSearchResult.getSearchEntries())
      {
        String destinationDn = DN.normalize(entry.getDN()).toLowerCase();
        
        if (destinationIgnoreDnSet.contains(destinationDn))
        {
          LOGGER.debug("IGNORE {}", destinationDn);
        }
        else
        {
          deleteDNs.add(destinationDn);
        }
      }

      LOGGER.info("# entries to delete = {}", deleteDNs.size());
      int deleteCounter = 0;

      for (int i = deleteDNs.size() - 1; i >= 0; i--)
      {
        String deleteDn = deleteDNs.get(i);

        LOGGER.info("{}/{} DELETE {}",
          ++deleteCounter, deleteDNs.size(), deleteDn);

        LDAPResult deleteResult = ConnectionHandler.getDestinationConnection().
          delete(deleteDn);
        
        if (deleteResult.getResultCode() != ResultCode.SUCCESS)
        {
          LOGGER.error("*** DELETE Failed");
          System.exit(0);
        }
      }
    }

    

    double endTimestamp = System.currentTimeMillis();
    LOGGER.info("deleteAllDestinationEntries() - done in {}s", (endTimestamp
      - startTimestamp) / 1000.0);
  }

  public static void synchronizeAllEntries() throws Throwable
  {
    LOGGER.info("synchronizeAllEntries()");
    ASN1GeneralizedTime soniaSyncTimestamp = new ASN1GeneralizedTime();
    double startTimestamp = System.currentTimeMillis();

    String baseDn = Ldapmigration.getConfig().getBaseDn();

    final HashSet<String> sourceIgnoreDnSet = new HashSet<>();
    final HashSet<String> destinationIgnoreDnSet = new HashSet<>();

    for (l9g.app.ldapmigration.config.LdapEntry entry
      : Ldapmigration.getConfig().getSourceIgnoreEntries())
    {
      sourceIgnoreDnSet.add(DN.normalize(entry.getDn()).toLowerCase());
    }

    for (l9g.app.ldapmigration.config.LdapEntry entry
      : Ldapmigration.getConfig().getDestinationIgnoreEntries())
    {
      destinationIgnoreDnSet.add(DN.normalize(entry.getDn()).toLowerCase());
    }

    SearchRequest searchRequest = new SearchRequest(baseDn, SearchScope.SUB,
      "|(objectClass=*)(objectClass=ldapSubEntry)", "*", "nsRoleDN");

    LOGGER.info("Start source search.");

    SearchResult sourceSearchResult = ConnectionHandler.getSourceConnection().
      search(searchRequest);

    int sourceEntries = sourceSearchResult.getEntryCount();

    ArrayList<Entry> sourceEntryList = new ArrayList<>();

    if (sourceEntries > 0)
    {
      LOGGER.debug("build hashset and list from source DNs");

      for (Entry entry : sourceSearchResult.getSearchEntries())
      {
        String entryDn = DN.normalize(entry.getDN()).toLowerCase();

        if (!sourceIgnoreDnSet.contains(entryDn)
          && !destinationIgnoreDnSet.contains(entryDn)
          && !baseDn.equals(entryDn))
        {
          sourceEntryList.add(entry);
        }
      }
    }
    else
    {
      LOGGER.error("No entries found");
      System.exit(0);
    }

    double endTimestamp = System.currentTimeMillis();

    LOGGER.info("# entries in source ldap = {} read in {}s", sourceEntries,
      (endTimestamp - startTimestamp) / 1000.0);

    startTimestamp = endTimestamp;

    LDAPConnection destinationConnection = ConnectionHandler.
      getDestinationConnection();

    int addCounter = 0;

    for (Entry entry : sourceEntryList)
    {
      if (addCounter % 1000 == 0)
      {
        LOGGER.info("{}/{}", addCounter, sourceEntryList.size());
      }

      addCounter++;

      LOGGER.debug("ADDING {}", entry.getDN());
    
      LDAPResult addResult = destinationConnection.add(entry);
      if (addResult.getResultCode() != ResultCode.SUCCESS)
      {
        LOGGER.error("ERROR adding entry {}", entry.getDN());
        System.exit(0);
      }
    }

    SyncTimestampUtil.set(soniaSyncTimestamp);
    endTimestamp = System.currentTimeMillis();
    LOGGER.info("synchronizeAllEntries() - done in {}s", (endTimestamp
      - startTimestamp) / 1000.0);
  }
}
