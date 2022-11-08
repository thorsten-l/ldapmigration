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
package l9g.app.ldapmigration.config;

//~--- JDK imports ------------------------------------------------------------
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.de)
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Configuration
{
  private String timezone;

  private String baseDn;
  
  private LdapHost sourceHost;
  private LdapHost destinationHost;
  
  private LdapEntry syncTimestampEntry;
  
  @XmlElementWrapper(name="sourceIgnoreEntries")
  @XmlElement(name="ldapEntry")
  private List<LdapEntry> sourceIgnoreEntries;
  
  @XmlElementWrapper(name="destinationIgnoreEntries")
  @XmlElement(name="ldapEntry")
  private List<LdapEntry> destinationIgnoreEntries;
}
