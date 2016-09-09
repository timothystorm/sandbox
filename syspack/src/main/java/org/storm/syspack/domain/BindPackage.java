package org.storm.syspack.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Contains the elements of a bind package
 *
 * @author Timothy Storm
 */
public class BindPackage implements Comparable<BindPackage> {
  private LocalDate    _lastUsed;
  private final String _name;
  private String       _contoken;

  /** unique tables used in the bind package */
  private Set<String>  _tables = new LinkedHashSet<>();

  public BindPackage(String name) {
    _name = name;
  }

  /**
   * Adds a table if it isn't already part of the package
   * 
   * @param tableName
   *          - to be added
   * @return this {@link BindPackage} for further configuration
   */
  public BindPackage addTable(String tableName) {
    _tables.add(tableName);
    return this;
  }

  @Override
  public int compareTo(BindPackage other) {
    CompareToBuilder compare = new CompareToBuilder();
    compare.append(getName(), other.getName());
    compare.append(getTables(), other.getTables());
    compare.append(getContoken(), other.getContoken());
    compare.append(getLastUsed(), other.getLastUsed());
    return compare.toComparison();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (!(obj instanceof BindPackage)) return false;
    if (obj == this) return true;

    BindPackage other = (BindPackage) obj;
    EqualsBuilder equals = new EqualsBuilder();
    equals.append(getName(), other.getName());
    equals.append(getTables(), other.getTables());
    equals.append(getContoken(), other.getContoken());
    equals.append(getLastUsed(), other.getLastUsed());
    return equals.isEquals();
  }

  public String getContoken() {
    return _contoken;
  }

  public LocalDate getLastUsed() {
    return _lastUsed;
  }

  public String getName() {
    return _name;
  }

  public Collection<String> getTables() {
    return Collections.unmodifiableCollection(_tables);
  }

  @Override
  public int hashCode() {
    HashCodeBuilder hash = new HashCodeBuilder(7, 31);
    hash.append(getName());
    hash.append(getTables());
    hash.append(getContoken());
    hash.append(getLastUsed());
    return hash.toHashCode();
  }

  public void setContoken(String contoken) {
    _contoken = contoken;
  }

  public void setLastUsed(LocalDate lastUsed) {
    _lastUsed = lastUsed;
  }

  @Override
  public String toString() {
    ToStringBuilder str = new ToStringBuilder(this, ToStringStyle.JSON_STYLE);
    str.append("name", getName());
    str.append("tables", getTables());
    str.append("contoken", getContoken());
    str.append("lastUsed", getLastUsed());
    return str.toString();
  }
}
