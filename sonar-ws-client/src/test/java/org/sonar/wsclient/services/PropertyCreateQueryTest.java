/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.wsclient.services;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class PropertyCreateQueryTest extends QueryTestCase {

  @Test
  public void create() {
    PropertyCreateQuery query = new PropertyCreateQuery("foo", "bar");
    assertThat(query.getUrl(), is("/api/properties?id=foo&value=bar&"));
    assertThat(query.getModelClass().getName(), is(Property.class.getName()));
  }

  @Test
  public void createForResource() {
    PropertyCreateQuery query = new PropertyCreateQuery("foo", "bar", "my:resource");
    assertThat(query.getUrl(), is("/api/properties?id=foo&value=bar&resource=my%3Aresource&"));
    assertThat(query.getModelClass().getName(), is(Property.class.getName()));
  }
}
