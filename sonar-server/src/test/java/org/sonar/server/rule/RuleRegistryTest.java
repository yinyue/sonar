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

package org.sonar.server.rule;

import com.github.tlrx.elasticsearch.test.EsSetup;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.SqlSession;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.rule.*;
import org.sonar.server.es.ESIndex;
import org.sonar.server.es.ESNode;
import org.sonar.test.TestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RuleRegistryTest {

  EsSetup esSetup;

  ESIndex searchIndex;

  @Mock
  RuleDao ruleDao;

  @Mock
  MyBatis myBatis;

  @Mock
  SqlSession session;

  RuleRegistry registry;

  @Before
  public void setUp() throws Exception {
    when(myBatis.openSession()).thenReturn(session);

    esSetup = new EsSetup(ImmutableSettings.builder().loadFromUrl(ESNode.class.getResource("config/elasticsearch.json"))
      .build());
    esSetup.execute(EsSetup.deleteAll());

    ESNode node = mock(ESNode.class);
    when(node.client()).thenReturn(esSetup.client());

    Settings settings = new Settings();
    settings.setProperty("sonar.log.profilingLevel", "FULL");
    Profiling profiling = new Profiling(settings);
    searchIndex = new ESIndex(node, profiling);
    searchIndex.start();

    registry = new RuleRegistry(searchIndex, ruleDao);
    registry.start();

    esSetup.execute(
      EsSetup.index("rules", "rule", "1").withSource(testFileAsString("shared/rule1.json")),
      EsSetup.index("rules", "rule", "2").withSource(testFileAsString("shared/rule2.json")),
      EsSetup.index("rules", "rule", "3").withSource(testFileAsString("shared/rule3.json"))
    );
    esSetup.client().admin().cluster().prepareHealth(RuleRegistry.INDEX_RULES).setWaitForGreenStatus().execute().actionGet();
  }

  @After
  public void tearDown() {
    searchIndex.stop();
    esSetup.terminate();
  }

  @Test
  public void should_register_mapping_at_startup() {
    assertThat(esSetup.exists("rules")).isTrue();
    assertThat(esSetup.client().admin().indices().prepareTypesExists("rules").setTypes("rule").execute().actionGet().isExists()).isTrue();
  }

  @Test
  public void should_find_rule_by_key() {
    assertThat(registry.findByKey(RuleKey.of("unknown", "RuleWithParameters"))).isNull();
    assertThat(registry.findByKey(RuleKey.of("xoo", "unknown"))).isNull();
    final Rule rule = registry.findByKey(RuleKey.of("xoo", "RuleWithParameters"));
    assertThat(rule).isNotNull();
    assertThat(rule.ruleKey().repository()).isEqualTo("xoo");
    assertThat(rule.ruleKey().rule()).isEqualTo("RuleWithParameters");
    assertThat(rule.params()).hasSize(5);
    assertThat(rule.adminTags()).hasSize(1);
    assertThat(rule.systemTags()).hasSize(2);
  }

  @Test
  public void should_filter_removed_rules() {
    assertThat(registry.findIds(new HashMap<String, String>())).containsOnly(1, 2);
  }

  @Test
  public void should_display_disabled_rule() {
    assertThat(registry.findIds(ImmutableMap.of("status", "BETA|REMOVED"))).containsOnly(2, 3);
  }

  @Test
  public void should_filter_on_name_or_key() throws Exception {
    assertThat(registry.findIds(ImmutableMap.of("nameOrKey", "parameters"))).containsOnly(1);
    assertThat(registry.findIds(ImmutableMap.of("nameOrKey", "issue"))).containsOnly(1, 2);
    assertThat(registry.findIds(ImmutableMap.of("nameOrKey", "issue line"))).containsOnly(2);
  }

  @Test
  public void should_filter_on_key() throws Exception {
    assertThat(registry.findIds(ImmutableMap.of("key", "OneIssuePerLine"))).containsOnly(2);
  }

  @Test
  public void should_filter_on_multiple_criteria() {
    assertThat(registry.findIds(ImmutableMap.of("nameOrKey", "parameters", "key", "OneIssuePerLine"))).isEmpty();
    assertThat(registry.findIds(ImmutableMap.of("repositoryKey", "polop"))).isEmpty();

    assertThat(registry.findIds(ImmutableMap.of("nameOrKey", "parameters", "repositoryKey", "xoo"))).containsOnly(1);
  }

  @Test
  public void should_filter_on_multiple_values() {
    assertThat(registry.findIds(ImmutableMap.of("key", "RuleWithParameters|OneIssuePerLine"))).hasSize(2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_reject_leading_wildcard() {
    registry.findIds(ImmutableMap.of("nameOrKey", "*ssue"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_wrap_parse_exceptions() {
    registry.findIds(ImmutableMap.of("nameOrKey", "\"'"));
  }

  @Test
  public void should_remove_all_rules_when_ro_rule_registered() {
    List<RuleDto> rules = Lists.newArrayList();
    registry.bulkRegisterRules(rules, null, null);
    assertThat(registry.findIds(new HashMap<String, String>())).hasSize(0);
  }

  @Test
  public void should_index_all_rules() {
    int ruleId1 = 3;
    RuleDto rule1 = new RuleDto();
    rule1.setRepositoryKey("repo");
    rule1.setRuleKey("key1");
    rule1.setId(ruleId1);
    rule1.setSeverity(Severity.MINOR);
    rule1.setNoteData("noteData");
    rule1.setNoteUserLogin("userLogin");
    int ruleId2 = 4;
    RuleDto rule2 = new RuleDto();
    rule2.setRepositoryKey("repo");
    rule2.setRuleKey("key2");
    rule2.setId(ruleId2);
    rule2.setSeverity(Severity.MAJOR);
    rule2.setParentId(ruleId1);
    List<RuleDto> rules = ImmutableList.of(rule1, rule2);

    RuleParamDto paramRule2 = new RuleParamDto();
    paramRule2.setName("name");
    paramRule2.setRuleId(ruleId2);
    Multimap<Integer, RuleParamDto> params = ArrayListMultimap.create();
    params.put(ruleId2, paramRule2);

    RuleRuleTagDto systemTag1Rule2 = new RuleRuleTagDto();
    systemTag1Rule2.setRuleId(ruleId2);
    systemTag1Rule2.setTag("tag1");
    systemTag1Rule2.setType(RuleTagType.SYSTEM);
    RuleRuleTagDto systemTag2Rule2 = new RuleRuleTagDto();
    systemTag2Rule2.setRuleId(ruleId2);
    systemTag2Rule2.setTag("tag2");
    systemTag2Rule2.setType(RuleTagType.SYSTEM);
    RuleRuleTagDto adminTagRule2 = new RuleRuleTagDto();
    adminTagRule2.setRuleId(ruleId2);
    adminTagRule2.setTag("tag");
    adminTagRule2.setType(RuleTagType.ADMIN);
    Multimap<Integer, RuleRuleTagDto> tags = ArrayListMultimap.create();
    tags.put(ruleId2, systemTag1Rule2);
    tags.put(ruleId2, systemTag2Rule2);
    tags.put(ruleId2, adminTagRule2);

    registry.bulkRegisterRules(rules, params, tags);
    assertThat(registry.findIds(ImmutableMap.of("repositoryKey", "repo"))).hasSize(2);

    Map<String, Object> rule2Document = esSetup.client().prepareGet("rules", "rule", Integer.toString(ruleId2))
      .execute().actionGet().getSourceAsMap();
    assertThat((List<String>) rule2Document.get(RuleDocument.FIELD_SYSTEM_TAGS)).hasSize(2);
    assertThat((List<String>) rule2Document.get(RuleDocument.FIELD_ADMIN_TAGS)).hasSize(1);
  }

  @Test
  public void should_index_and_reindex_single_rule() {
    RuleDto rule = new RuleDto();
    rule.setRepositoryKey("repo");
    rule.setRuleKey("key");
    rule.setSeverity(Severity.MINOR);
    int id = 3;
    rule.setId(id);
    when(ruleDao.selectById(id)).thenReturn(rule);
    registry.saveOrUpdate(id);
    assertThat(registry.findIds(ImmutableMap.of("repositoryKey", "repo"))).hasSize(1);
    rule.setName("polop");
    registry.saveOrUpdate(id);
    assertThat(registry.findIds(ImmutableMap.of("repositoryKey", "repo"))).hasSize(1);
  }

  @Test
  public void should_update_existing_rules_and_forget_deleted_rules() {
    int ruleId1 = 1;
    RuleDto rule1 = new RuleDto();
    rule1.setRepositoryKey("xoo");
    rule1.setRuleKey("key1");
    rule1.setId(ruleId1);
    rule1.setSeverity(Severity.MINOR);
    int ruleId2 = 2;
    RuleDto rule2 = new RuleDto();
    rule2.setRepositoryKey("xoo");
    rule2.setRuleKey("key2");
    rule2.setId(ruleId2);
    rule2.setParentId(ruleId1);
    rule2.setSeverity(Severity.MINOR);
    List<RuleDto> rules = ImmutableList.of(rule1, rule2);

    assertThat(esSetup.exists("rules", "rule", "3")).isTrue();
    when(ruleDao.selectNonManual(any(SqlSession.class))).thenReturn(rules);
    final Multimap<Integer, RuleParamDto> params = ArrayListMultimap.create();
    final Multimap<Integer, RuleRuleTagDto> tags = ArrayListMultimap.create();
    registry.bulkRegisterRules(rules, params, tags);

    assertThat(registry.findIds(ImmutableMap.of("repositoryKey", "xoo")))
      .hasSize(2)
      .containsOnly(ruleId1, ruleId2);
    assertThat(esSetup.exists("rules", "rule", "3")).isFalse();
  }

  @Test
  public void should_find_rules_by_name() {
    // Removed rule should not appear
    assertThat(registry.find(RuleQuery.builder().withPage(1).withPageSize(10).build()).results()).hasSize(2);

    // Search is case insensitive
    assertThat(registry.find(RuleQuery.builder().withPage(1).withPageSize(10).withSearchQuery("one issue per line").build()).results()).hasSize(1);

    // Search is ngram based
    assertThat(registry.find(RuleQuery.builder().withPage(1).withPageSize(10).withSearchQuery("with param").build()).results()).hasSize(1);

    // Search works also with key
    assertThat(registry.find(RuleQuery.builder().withPage(1).withPageSize(10).withSearchQuery("OneIssuePerLine").build()).results()).hasSize(1);

  }

  private String testFileAsString(String testFile) throws Exception {
    return IOUtils.toString(TestUtils.getResource(getClass(), testFile).toURI());
  }

}
