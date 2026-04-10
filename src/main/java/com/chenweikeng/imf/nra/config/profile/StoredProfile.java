package com.chenweikeng.imf.nra.config.profile;

import com.chenweikeng.imf.nra.config.ConfigSetting;
import com.chenweikeng.imf.nra.config.ModConfig;
import java.util.Objects;
import java.util.UUID;

public class StoredProfile {
  public String id;
  public String name;
  public String description;
  public long createdAt;
  public long modifiedAt;
  public ConfigSetting data;

  public StoredProfile() {}

  public StoredProfile(String id, String name, String description, ConfigSetting data) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.createdAt = System.currentTimeMillis();
    this.modifiedAt = this.createdAt;
    this.data = data;
  }

  public static StoredProfile fromCurrentConfig(String name, String description) {
    String id = UUID.randomUUID().toString();
    ConfigSetting dataCopy = ModConfig.currentSetting.copy();
    return new StoredProfile(id, name, description, dataCopy);
  }

  public static StoredProfile create(
      String id, String name, String description, ConfigSetting data) {
    return new StoredProfile(id, name, description, data);
  }

  public StoredProfile copy() {
    StoredProfile copy = new StoredProfile();
    copy.id = this.id;
    copy.name = this.name;
    copy.description = this.description;
    copy.createdAt = this.createdAt;
    copy.modifiedAt = this.modifiedAt;
    copy.data = this.data != null ? this.data.copy() : null;
    return copy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StoredProfile that = (StoredProfile) o;
    return createdAt == that.createdAt
        && modifiedAt == that.modifiedAt
        && Objects.equals(id, that.id)
        && Objects.equals(name, that.name)
        && Objects.equals(description, that.description)
        && Objects.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description, createdAt, modifiedAt, data);
  }
}
