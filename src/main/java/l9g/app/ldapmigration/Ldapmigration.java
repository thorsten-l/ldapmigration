package l9g.app.ldapmigration;

import jakarta.xml.bind.JAXB;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import l9g.app.ldapmigration.config.Configuration;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
public class Ldapmigration
{
  private final static Logger LOGGER = LoggerFactory.getLogger(
    Ldapmigration.class.getName());

  private static final String CONFIGURATION = "config.xml";

  @Getter
  private static Configuration config;

  public static void buildInfo(PrintStream out)
  {
    BuildProperties build = BuildProperties.getInstance();
    out.println("Project Name    : " + build.getProjectName());
    out.println("Project Version : " + build.getProjectVersion());
    out.println("Build Timestamp : " + build.getTimestamp());
    out.flush();
  }

  public static void readConfiguration()
  {
    LOGGER.debug("reading configuration file config.xml");

    try
    {
      Configuration c = null;
      File configFile = new File(CONFIGURATION);

      LOGGER.debug("Config file: {}", configFile.getAbsolutePath());

      if (configFile.exists() && configFile.canRead())
      {
        c = JAXB.unmarshal(new FileReader(configFile), Configuration.class);

        LOGGER.debug("new config <{}>", c);

        if (c != null)
        {
          LOGGER.debug("setting config");
          config = c;
        }
      }
      else
      {
        LOGGER.error("Can NOT read config file");
      }

      if (config == null)
      {
        LOGGER.error("config file NOT found");
        System.exit(0);
      }
    }
    catch (Exception e)
    {
      LOGGER.error("Configuratione file config.xml not found ", e);
      System.exit(0);
    }
  }

  public static void main(String[] args) throws Throwable
  {
    buildInfo(System.out);
    readConfiguration();

    boolean initialSync = false;
    
    if ( initialSync )
    {
      SyncTimestampUtil.add();
      SyncBaseDn.synchronizeGeneralAttributes();
      SyncLdapInitial.deleteAllDestinationEntries();
      SyncLdapInitial.synchronizeAllEntries();
      SyncBaseDn.synchronizeACIs();
    }
    else
    {
      SyncBaseDn.synchronizeGeneralAttributes();
      SyncLdap.synchronizeGeneralAttributesInclusiveNsRoleDN();
      SyncBaseDn.synchronizeACIs();
    }
  }
}
