package l9g.app.ldapmigration.config;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
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
public class LdapEntry
{
  @XmlAttribute
  @XmlJavaTypeAdapter(l9g.app.ldapmigration.config.XmlMatchTypeAdapter.class)
  private MatchType match;

  private String dn;

  private String attributeName;
}
