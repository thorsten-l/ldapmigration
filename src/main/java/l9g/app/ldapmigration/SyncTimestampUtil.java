package l9g.app.ldapmigration;

import com.unboundid.asn1.ASN1GeneralizedTime;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.SearchResultEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
public class SyncTimestampUtil
{
  private final static Logger LOGGER = LoggerFactory.getLogger(
    SyncTimestampUtil.class.getName());

  private SyncTimestampUtil()
  {
  }

  public static ASN1GeneralizedTime get() throws Throwable
  {
    String timestampDn = Ldapmigration.getConfig()
      .getSyncTimestampEntry().getDn();
    String timestampAttributeName = Ldapmigration.getConfig().
      getSyncTimestampEntry().getAttributeName();

    SearchResultEntry entry = ConnectionHandler.getDestinationConnection().
      getEntry(timestampDn, timestampAttributeName);

    ASN1GeneralizedTime timestamp = new ASN1GeneralizedTime(entry.
      getAttributeValueAsDate(timestampAttributeName));

    LOGGER.debug("timestamp dn: {} ({})", timestampDn, timestamp);

    return timestamp;
  }

  public static void set(ASN1GeneralizedTime timestamp) throws Throwable
  {
    String timestampDn = Ldapmigration.getConfig()
      .getSyncTimestampEntry().getDn();
    String timestampAttributeName = Ldapmigration.getConfig().
      getSyncTimestampEntry().getAttributeName();

    LOGGER.debug("timestamp dn: {} ({}) attrib={}",
      timestampDn, timestamp, timestampAttributeName);

    Modification modification = new Modification(ModificationType.REPLACE,
      timestampAttributeName, timestamp.getStringRepresentation());

    ModifyRequest modifyRequest = new ModifyRequest(timestampDn,
      modification);

    LDAPResult ldapResult = ConnectionHandler.getDestinationConnection()
      .modify(modifyRequest);

    LOGGER.debug("timestamp dn: {} ({}) : {}",
      timestampDn, timestamp, ldapResult.getResultCode());
  }

  public static void add() throws Throwable
  {
    ASN1GeneralizedTime timestamp = new ASN1GeneralizedTime();

    String timestampDn = Ldapmigration.getConfig()
      .getSyncTimestampEntry().getDn();
    String timestampAttributeName = Ldapmigration.getConfig().
      getSyncTimestampEntry().getAttributeName();

    LOGGER.debug("timestamp dn: {} ({}) attrib={}",
      timestampDn, timestamp, timestampAttributeName);

    try
    {
      ConnectionHandler
        .getDestinationConnection().delete(timestampDn);
    }
    catch (Throwable t)
    {
      LOGGER.debug( "Entry {} not exists", timestampDn );
    }

    Entry entry = new Entry(DN.normalize(timestampDn),
      new Attribute("objectClass", "top"),
      new Attribute("objectClass", "pab"),
      new Attribute("objectClass", "soniaSync"),
      new Attribute("cn", "ldapmigration"),
      new Attribute(timestampAttributeName, timestamp.getStringRepresentation())
    );

    LDAPResult ldapResult = ConnectionHandler
      .getDestinationConnection().add(entry);

    LOGGER.debug("timestamp dn: {} ({}) : {}",
      timestampDn, timestamp, ldapResult.getResultCode());
  }

}
