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
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import l9g.app.ldapmigration.config.LdapEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
public class SyncLdap
{
  private final static Logger LOGGER = LoggerFactory.getLogger(SyncLdap.class.
    getName());

  public static void synchronizeGeneralAttributesInclusiveNsRoleDN(ASN1GeneralizedTime soniaSyncTimestamp) throws
    Throwable
  {
    LOGGER.info("synchronizeGeneralAttributesInclusiveNsRoleDN()");
    String baseDn = Ldapmigration.getConfig().getBaseDn();

    // ---------------
    SearchRequest searchRequest = new SearchRequest(baseDn, SearchScope.SUB,
      "(|(objectClass=*)(objectClass=ldapSubEntry))", "dn");

    LOGGER.info("Start source search.");

    double startTimestamp = System.currentTimeMillis();
    SearchResult sourceSearchResult = ConnectionHandler.getSourceConnection().
      search(searchRequest);
    double endTimestamp = System.currentTimeMillis();
    LOGGER.info("done time={}", (endTimestamp - startTimestamp) / 1000.0);
    startTimestamp = endTimestamp;
    LOGGER.info("Start destination search.");
    SearchResult destinationSearchResult = ConnectionHandler.
      getDestinationConnection().search(
        searchRequest);

    endTimestamp = System.currentTimeMillis();
    LOGGER.info("done time={}", (endTimestamp - startTimestamp) / 1000.0);

    int sourceEntries = sourceSearchResult.getEntryCount();
    int destinationEntries = destinationSearchResult.getEntryCount();

    LOGGER.debug("build hashset and list from source/destination DNs");

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

    LOGGER.info("# entries in source ldap = {}", sourceEntries);
    LOGGER.info("# entries in destination ldap = {}", destinationEntries);

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

    startTimestamp = System.currentTimeMillis();

    if (destinationSearchResult.getResultCode() == ResultCode.SUCCESS)
    {
      ArrayList<String> deleteDNs = new ArrayList<>();

      for (String destinationDn : destinationDnList)
      {
        if (destinationIgnoredEntrySet.stream().anyMatch(ignoredEntry -> ignoredEntry.matchesDn(destinationDn)))
        {
          LOGGER.debug("IGNORE {}", destinationDn);
        }
        else
        {
          if (!sourceDnSet.contains(destinationDn))
          {
            deleteDNs.add(destinationDn);
          }
        }
      }

      // DELETE 
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
    LOGGER.info("delete done time={}s", (endTimestamp - startTimestamp) / 1000.0);

    //////////////////////////////////////////////////  
    LOGGER.info("search entries to update");

    // UPDATING
    ASN1GeneralizedTime lastSyncTimestamp = SyncTimestampUtil.get();

    searchRequest = new SearchRequest(baseDn, SearchScope.SUB,
      "(&(|(objectClass=*)(objectClass=ldapSubEntry))(modifyTimestamp>="
      + lastSyncTimestamp.getStringRepresentation() + "))", "*", "nsRoleDn");

    SearchResult searchResult = ConnectionHandler.getSourceConnection().search(
      searchRequest);

    LOGGER.info("# entries to update = {}", searchResult.getEntryCount());

    int counter = 0;

    if (searchResult.getResultCode() == ResultCode.SUCCESS)
    {
      for (SearchResultEntry entry : searchResult.getSearchEntries())
      {
        counter++;

        if (DN.normalize(entry.getDN()).equals(DN.normalize(baseDn)))
        {
          LOGGER.debug("{} SKIP {}", counter, baseDn);
        }
        else
        {
          SearchResultEntry destinationEntry = ConnectionHandler.
            getDestinationConnection().getEntry(
              entry.getDN(), "*", "nsRoleDn");

          if (destinationEntry != null)
          {
            List<Modification> modifications
              = Entry.diff(destinationEntry, entry, true);

            // modify
            if (modifications != null && modifications.size() > 0)
            {
              LOGGER.info("{} MODIFY {}", counter, destinationEntry.getDN());

              if (LOGGER.isDebugEnabled())
              {
                for (Modification modification : modifications)
                {
                  LOGGER.debug("{}", modification);
                }
              }

              LDAPResult modifyResult = ConnectionHandler.
                getDestinationConnection().modify(
                  destinationEntry.getDN(), modifications);

              if (modifyResult.getResultCode() != ResultCode.SUCCESS)
              {
                LOGGER.error("MODIFY FAILED");
                System.exit(0);
              }
            }
            else
            {
              LOGGER.debug("{} NO MODIFICATION {}", counter, destinationEntry.
                getDN());
            }
          }
        }
      }
    }

    ////////////////////////
    // ADDING
    
    LOGGER.info( "adding new entries");
    
    int addCounter = 0;

    for (String sourceDn : sourceDnList)
    {
      if (!destinationDnSet.contains(sourceDn)
        && !sourceIgnoredEntrySet.stream().anyMatch(ignoredEntry -> ignoredEntry.matchesDn(sourceDn)))
      {
        LOGGER.info("{} ADDING {}", ++addCounter, sourceDn);

        LDAPResult addResult = ConnectionHandler.getDestinationConnection().add(
          ConnectionHandler.getSourceConnection().getEntry(sourceDn, "*", "nsRoleDn"));

        if (addResult.getResultCode() != ResultCode.SUCCESS)
        {
          LOGGER.error("ADD Failed");
          System.exit(0);
        }
      }
    }

    LOGGER.info("synchronizeGeneralAttributesInclusiveNsRoleDN() - done");
  }
}
