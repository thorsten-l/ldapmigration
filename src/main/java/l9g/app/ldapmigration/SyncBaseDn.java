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

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
public class SyncBaseDn
{
  private final static Logger LOGGER = LoggerFactory.getLogger(
    SyncBaseDn.class.getName());

  public static void synchronizeGeneralAttributes() throws Throwable
  {
    LOGGER.info("synchronizeGeneralAttributes()");
    String baseDn = Ldapmigration.getConfig().getBaseDn();

    Entry sourceEntry = ConnectionHandler
      .getSourceConnection().getEntry(baseDn);
    Entry destinationEntry = ConnectionHandler
      .getDestinationConnection().getEntry(baseDn);

    if (sourceEntry != null && destinationEntry != null)
    {
      List<Modification> modifications = Entry.diff(destinationEntry,
        sourceEntry, true);

      if (LOGGER.isDebugEnabled())
      {
        for (Modification modification : modifications)
        {
          LOGGER.debug("{}", modification);
        }
      }

      if (!modifications.isEmpty())
      {
        LDAPResult modifyResult = ConnectionHandler
          .getDestinationConnection().modify(baseDn, modifications);

        if (modifyResult.getResultCode() != ResultCode.SUCCESS)
        {
          System.out.println("MODIFY FAILED");
          System.exit(1);
        }
      }
      else
      {
        LOGGER.info("No modifications found.");
      }
    }
    else
    {
      LOGGER.info("Base DN entries not found");
    }
    LOGGER.info("synchronizeGeneralAttributes() - done");
  }

  public static void synchronizeACIs() throws Throwable
  {
    LOGGER.info("synchronizeACIs()");

    String baseDn = Ldapmigration.getConfig().getBaseDn();

    SearchRequest searchRequest = new SearchRequest(baseDn,
      SearchScope.BASE, "objectClass=*", "aci");

    SearchResult searchResult = ConnectionHandler.getSourceConnection().search(
      searchRequest);

    if (searchResult.getEntryCount() == 1)
    {
      SearchResultEntry entry = searchResult.getSearchEntries().get(0);
      Attribute acis = entry.getAttribute("aci");

      ArrayList<String> valueList = new ArrayList<>();

      try ( PrintWriter out = new PrintWriter(new FileWriter("logs/aci-sync.log", true)))
      {
        out.println( "\n" + LocalDateTime.now() + "--------------------------");
        
        if( acis != null )
        {
          LOGGER.info( "# of acis = {}", acis.size());
          int counter = 0;
          for (String value : acis.getValues())
          {
            ++counter;
            value = value.trim();
            valueList.add(value);

            Modification modification = new Modification(ModificationType.REPLACE,
              "aci", valueList.toArray(new String[0]));

            ModifyRequest modifyRequest = new ModifyRequest(entry.getDN(),
              modification);

            LDAPResult ldapResult = null;

            try
            {
              ldapResult = ConnectionHandler.getDestinationConnection().modify(
                modifyRequest);
              if (ldapResult.getResultCode() != ResultCode.SUCCESS)
              {
                out.println( counter + " FR>  " + value);
                out.println(ldapResult);
                LOGGER.error("{} FR> {}\n{}", counter, value, ldapResult);
              }
              else
              {
                out.println(counter + " S> " + value);
                LOGGER.debug("{} S> {}", counter, value);
              }
            }
            catch (Throwable e)
            {
              out.println(counter + " FE>  " + value);
              out.println("   - " + e.getMessage());
              LOGGER.error("{} FE> {}\n{}", counter, value, e.getMessage());
              valueList.remove(value);
            }
          }
        }
        else
        {
          LOGGER.info("no ACIs found on source, delete ACI attribute on target");
          out.println("no ACIs found");
          ConnectionHandler.getDestinationConnection().modify(
            entry.getDN(), new Modification(ModificationType.REPLACE, "aci"));
        }
      }
    }

    LOGGER.info("synchronizeACIs() - done");
  }
}
