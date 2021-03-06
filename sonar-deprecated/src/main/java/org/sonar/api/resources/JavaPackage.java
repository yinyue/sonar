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
package org.sonar.api.resources;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * A class that represents a Java package in Sonar
 *
 * @since 1.10
 * @deprecated since 4.2 use {@link Directory} instead
 */
@Deprecated
public class JavaPackage extends Resource {

  /**
   * Default package name for classes without package definition
   */
  public static final String DEFAULT_PACKAGE_NAME = "[default]";

  /**
   * Default constructor
   * @deprecated since 4.2 use {@link #create(String, String)}
   */
  @Deprecated
  public JavaPackage() {
    this(null);
  }

  /**
   * Creates a JavaPackage from its key.
   * @deprecated since 4.2 use {@link #create(String, String)}
   */
  @Deprecated
  public JavaPackage(String deprecatedKey) {
    if (DEFAULT_PACKAGE_NAME.equals(deprecatedKey)) {
      deprecatedKey = Directory.ROOT;
    }
    String deprecatedDirectoryKey = StringUtils.trimToEmpty(deprecatedKey);
    deprecatedDirectoryKey = deprecatedDirectoryKey.replaceAll("\\.", Directory.SEPARATOR);
    setDeprecatedKey(StringUtils.defaultIfEmpty(deprecatedDirectoryKey, Directory.ROOT));
  }

  /**
   * @return whether the JavaPackage key is the default key
   */
  public boolean isDefault() {
    return StringUtils.equals(getDeprecatedKey(), Directory.ROOT);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean matchFilePattern(String antPattern) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDescription() {
    return null;
  }

  /**
   * @return SCOPE_SPACE
   */
  @Override
  public String getScope() {
    return Scopes.DIRECTORY;
  }

  /**
   * @return QUALIFIER_PACKAGE
   */
  @Override
  public String getQualifier() {
    return Qualifiers.DIRECTORY;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return getKey();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Resource getParent() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getLongName() {
    return null;
  }

  /**
   * @return null
   */
  @Override
  public Language getLanguage() {
    return null;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("id", getId())
      .append("key", getKey())
      .append("deprecatedKey", getDeprecatedKey())
      .toString();
  }
}
