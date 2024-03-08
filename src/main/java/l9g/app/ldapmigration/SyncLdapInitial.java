/*
 * Copyright 2022 Thorsten Ludewig (t.ludewig@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import l9g.app.ldapmigration.config.LdapEntry;
import l9g.app.ldapmigration.config.MatchType;
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
    double endTimestamp = 0;

    String baseDn = Ldapmigration.getConfig().getBaseDn();

    final HashSet<LdapEntry> destinationIgnoredEntrySet = new HashSet<>();

    for (l9g.app.ldapmigration.config.LdapEntry entry
      : Ldapmigration.getConfig().getDestinationIgnoreEntries())
    {
      destinationIgnoredEntrySet.add(entry);
    }
    
    destinationIgnoredEntrySet.add(new LdapEntry(MatchType.equals, baseDn, null));

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
        
        if (destinationIgnoredEntrySet.stream().anyMatch(ignoredEntry -> ignoredEntry.matchesDn(destinationDn)))
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

    

    endTimestamp = System.currentTimeMillis();
    LOGGER.info("deleteAllDestinationEntries() - done in {}s", (endTimestamp
      - startTimestamp) / 1000.0);
  }

  public static void synchronizeAllEntries()
  {
    LOGGER.info("synchronizeAllEntries()");
    ASN1GeneralizedTime soniaSyncTimestamp = new ASN1GeneralizedTime();
    double startTimestamp = System.currentTimeMillis();
    double endTimestamp = 0;
    String currentEntryDn = null;

    try
    {
    String baseDn = Ldapmigration.getConfig().getBaseDn();

    final HashSet<LdapEntry> sourceIgnoredEntrySet = new HashSet<>();
    final HashSet<LdapEntry> destinationIgnoredEntrySet = new HashSet<>();

    for (l9g.app.ldapmigration.config.LdapEntry entry
      : Ldapmigration.getConfig().getSourceIgnoreEntries())
    {
      sourceIgnoredEntrySet.add(entry);
    }

    for (l9g.app.ldapmigration.config.LdapEntry entry
      : Ldapmigration.getConfig().getDestinationIgnoreEntries())
    {
      destinationIgnoredEntrySet.add(entry);
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

        if (
          !sourceIgnoredEntrySet.stream().anyMatch(ignoredEntry -> ignoredEntry.matchesDn(entryDn))
          && !destinationIgnoredEntrySet.stream().anyMatch(ignoredEntry -> ignoredEntry.matchesDn(entryDn))
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

    endTimestamp = System.currentTimeMillis();

    LOGGER.info("# entries in source ldap = {} read in {}s", sourceEntries,
      (endTimestamp - startTimestamp) / 1000.0);

    startTimestamp = endTimestamp;

    LDAPConnection destinationConnection = ConnectionHandler.
      getDestinationConnection();

    int addCounter = 0;
    double intervalTimestamp = System.currentTimeMillis();

    for (Entry entry : sourceEntryList)
    {
      if (addCounter % 1000 == 0)
      {
        LOGGER.info("{}/{} read in {}s", addCounter, sourceEntryList.size(),
          (System.currentTimeMillis() - intervalTimestamp) / 1000.0);
        
        intervalTimestamp = System.currentTimeMillis();
      }

      addCounter++;

      currentEntryDn = entry.getDN();
      
      LOGGER.debug("ADDING {}", currentEntryDn );
    
      LDAPResult addResult = destinationConnection.add(entry);
      if (addResult.getResultCode() != ResultCode.SUCCESS)
      {
        LOGGER.error("ERROR adding entry {}", entry.getDN());
        System.exit(0);
      }
    }

    SyncTimestampUtil.set(soniaSyncTimestamp);
    
    }
    catch( Throwable t )
    {
      LOGGER.error("ERROR on {} : {}", currentEntryDn, t.getMessage());
      System.exit(0);
    }
    
    
    endTimestamp = System.currentTimeMillis();
    LOGGER.info("synchronizeAllEntries() - done in {}s", (endTimestamp
      - startTimestamp) / 1000.0);
  }
}
