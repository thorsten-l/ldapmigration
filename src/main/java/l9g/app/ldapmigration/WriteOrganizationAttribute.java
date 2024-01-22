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
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
public class WriteOrganizationAttribute
{
  private final static Logger LOGGER = LoggerFactory.getLogger(
    WriteOrganizationAttribute.class.
      getName());

  public static void writeOrganizationAttributeOnDestination() throws
    Throwable
  {
    LOGGER.info("writeOrganizationAttributeOnDestination()");
    String baseDn = Ldapmigration.getConfig().getBaseDn();

    ASN1GeneralizedTime soniaSyncTimestamp = new ASN1GeneralizedTime();

    // ---------------
    SearchRequest searchRequest = new SearchRequest(baseDn, SearchScope.SUB,
      "(objectClass=soniaAccount)", "dn");

    double startTimestamp = System.currentTimeMillis();

    // ---------------
    LOGGER.info("Start destination search.");
    SearchResult destinationSearchResult = ConnectionHandler.
      getDestinationConnection().search(
        searchRequest);

    int destinationEntries = destinationSearchResult.getEntryCount();

    LOGGER.debug("build list from estination DNs");

    ArrayList<Entry> destinationDnList = new ArrayList<>();
    for (Entry entry : destinationSearchResult.getSearchEntries())
    {
      destinationDnList.add(entry);
    }

    double endTimestamp = System.currentTimeMillis();
    LOGGER.info("done time={}", (endTimestamp - startTimestamp) / 1000.0);

    int counter = 0;
    
    for (Entry entry : destinationDnList)
    {
      String entryDN = DN.normalize(entry.getDN());
      String o = entryDN.substring(entryDN.indexOf("ou=people,") + 12);
      o = o.substring(0, o.indexOf(','));
      LOGGER.info("{}/{} {}: o={}", ++counter, destinationEntries, entryDN, o);

      Modification modification = new Modification(ModificationType.REPLACE, "o", o );

      
      LDAPResult modifyResult = ConnectionHandler.
        getDestinationConnection().modify(
          entryDN, modification);

      if (modifyResult.getResultCode() != ResultCode.SUCCESS)
      {
        LOGGER.error("MODIFY FAILED");
        System.exit(0);
      }
    }

    LOGGER.info("# entries in destination ldap = {}", destinationEntries);
    LOGGER.info("writeOrganizationAttributeOnDestination() - done");
  }
}
