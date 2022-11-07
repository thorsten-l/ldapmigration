package l9g.app.ldapmigration.config;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LdapHost
{
  private String hostname;
  private int port;
  boolean sslEnabled;
  private Credentials credentials;
}
