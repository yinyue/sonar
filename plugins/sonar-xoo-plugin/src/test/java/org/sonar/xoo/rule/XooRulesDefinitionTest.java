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
package org.sonar.xoo.rule;

import org.junit.Test;
import org.sonar.api.server.rule.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;

import static org.fest.assertions.Assertions.assertThat;

public class XooRulesDefinitionTest {

  @Test
  public void define_xoo_rules() {
    XooRulesDefinition def = new XooRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    def.define(context);

    RulesDefinition.Repository repo = context.repository("xoo");
    assertThat(repo).isNotNull();
    assertThat(repo.name()).isEqualTo("Xoo");
    assertThat(repo.language()).isEqualTo("xoo");
    assertThat(repo.rules()).hasSize(1);

    RulesDefinition.Rule x1 = repo.rule("x1");
    assertThat(x1.key()).isEqualTo("x1");
    assertThat(x1.tags()).containsOnly("style", "security");
    assertThat(x1.htmlDescription()).isNotEmpty();

    assertThat(x1.debtCharacteristic()).isEqualTo("INTEGRATION_TESTABILITY");
    assertThat(x1.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(x1.debtRemediationFunction().factor()).isEqualTo("1h");
    assertThat(x1.debtRemediationFunction().offset()).isEqualTo("30min");
  }
}
