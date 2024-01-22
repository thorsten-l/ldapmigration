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
@XmlAccessorType( XmlAccessType.FIELD )
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LdapEntry
{
  @XmlAttribute
  @XmlJavaTypeAdapter( l9g.app.ldapmigration.config.XmlMatchTypeAdapter.class )
  private MatchType match;

  @XmlAttribute
  private String dn;

  private String attributeName;

  public boolean matchesDn( String name )
  {
    boolean result = false;

    switch( match )
    {
      case equals:
        result = name.equals( dn );
        break;

      case contains:
        result = name.contains( dn );
        break;

      case startsWith:
        result = name.startsWith( dn );
        break;

      case endsWith:
        result = name.endsWith( dn );
    }

    return result;
  }

  public boolean matchesAttributeName( String name )
  {
    boolean result = false;

    switch( match )
    {
      case equals:
        result = name.equals( attributeName );
        break;

      case contains:
        result = name.contains( attributeName );
        break;

      case startsWith:
        result = name.startsWith( attributeName );
        break;

      case endsWith:
        result = name.endsWith( attributeName );
    }

    return result;
  }

}
