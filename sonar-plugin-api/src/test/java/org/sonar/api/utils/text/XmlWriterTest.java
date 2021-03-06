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
package org.sonar.api.utils.text;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.StringWriter;

import static org.fest.assertions.Assertions.assertThat;

public class XmlWriterTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  StringWriter xml = new StringWriter();
  XmlWriter writer = XmlWriter.of(xml);

  private void expect(String s) {
    assertThat(xml.toString()).isEqualTo(s);
  }

  @Test
  public void declaration() {
    writer.declaration().begin("foo").end().close();
    expect("<?xml version='1.0' encoding='UTF-8'?><foo/>");
  }

  @Test
  public void only_root() throws Exception {
    writer.begin("foo").end().close();
    expect("<foo/>");
  }

  @Test
  public void escape_value() throws Exception {
    writer.prop("foo", "1<2 & 2>=2").close();
    expect("<foo>1&lt;2 &amp; 2>=2</foo>");
  }

  @Test
  public void only_root_with_value() throws Exception {
    writer.prop("foo", "bar").close();
    expect("<foo>bar</foo>");
  }

  @Test
  public void ignore_null_values() throws Exception {
    writer.begin("root")
      .prop("nullNumber", (Number) null)
      .prop("nullString", (String) null)
      .end().close();
    expect("<root/>");
  }

  @Test
  public void fail_on_NaN_value() throws Exception {
    thrown.expect(WriterException.class);
    thrown.expectMessage("Fail to write XML. Double value is not valid: NaN");
    writer.begin("root").prop("foo", Double.NaN).end().close();
  }
}
