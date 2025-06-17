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
import jakarta.xml.bind.JAXB;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
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

  private static String CONFIGURATION = "config.xml";

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

    List<String> argsList = Arrays.asList(args);
    boolean initialSync = argsList.contains("-i");
    boolean updateBaseDn = argsList.contains("-b");
    boolean writeOrganizationAttributes = argsList.contains("-o");

    if( argsList.contains("-f") )
    {
      CONFIGURATION = argsList.get( argsList.indexOf("-f") + 1 );
    }

    readConfiguration();
    
    ASN1GeneralizedTime soniaSyncTimestamp = new ASN1GeneralizedTime();

    if (initialSync)
    {
      SyncTimestampUtil.add();

      SyncBaseDn.synchronizeGeneralAttributes();
      SyncLdapInitial.deleteAllDestinationEntries();
      SyncLdapInitial.synchronizeAllEntries();
      SyncBaseDn.synchronizeACIs();
    }
    else
    {
      if (writeOrganizationAttributes)
      {
        WriteOrganizationAttribute.writeOrganizationAttributeOnDestination();
      }
      else
      {
        if (updateBaseDn)
        {
          SyncBaseDn.synchronizeGeneralAttributes();
        }

        SyncLdap.synchronizeGeneralAttributesInclusiveNsRoleDN();

        if (updateBaseDn)
        {
          SyncBaseDn.synchronizeACIs();
        }
      }
    }
    
    SyncTimestampUtil.set(soniaSyncTimestamp);
  }
}
