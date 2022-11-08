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

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import java.security.GeneralSecurityException;
import javax.net.ssl.SSLSocketFactory;
import l9g.app.ldapmigration.config.LdapHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
public class ConnectionHandler
{
  private final static Logger LOGGER = LoggerFactory.getLogger(
    ConnectionHandler.class.getName());

  private LDAPConnection sourceConnection;

  private LDAPConnection destinationConnection;

  private final static ConnectionHandler SINGLETON = new ConnectionHandler();

  private ConnectionHandler()
  {
    LOGGER.debug("ConnectionHandler()");
  }

  public synchronized LDAPConnection _getSourceConnection() throws Throwable
  {
    if (sourceConnection == null)
    {
      sourceConnection = getConnection(Ldapmigration.getConfig()
        .getSourceHost());
    }
    return sourceConnection;
  }

  public synchronized LDAPConnection _getDestinationConnection() throws
    Throwable
  {
    if (destinationConnection == null)
    {
      destinationConnection = getConnection(Ldapmigration.getConfig()
        .getDestinationHost());
    }
    return destinationConnection;
  }

  public static LDAPConnection getSourceConnection() throws Throwable
  {
    return SINGLETON._getSourceConnection();
  }

  public static LDAPConnection getDestinationConnection() throws Throwable
  {
    return SINGLETON._getDestinationConnection();
  }

  private LDAPConnection getConnection(LdapHost ldapHost) throws Exception
  {
    LDAPConnection ldapConnection;

    LDAPConnectionOptions options = new LDAPConnectionOptions();
    if (ldapHost.isSslEnabled())
    {
      ldapConnection = new LDAPConnection(createSSLSocketFactory(), options,
        ldapHost.getHostname(), ldapHost.getPort(),
        ldapHost.getCredentials().getBindDN(),
        ldapHost.getCredentials().getPassword());
    }
    else
    {
      ldapConnection = new LDAPConnection(options,
        ldapHost.getHostname(), ldapHost.getPort(),
        ldapHost.getCredentials().getBindDN(),
        ldapHost.getCredentials().getPassword());
    }
    ldapConnection.setConnectionName(ldapHost.getHostname());
    return ldapConnection;
  }

  private SSLSocketFactory createSSLSocketFactory() throws
    GeneralSecurityException
  {
    SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
    return sslUtil.createSSLSocketFactory();
  }
}
