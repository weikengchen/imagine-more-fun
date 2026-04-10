package com.chenweikeng.imf.nra.config.profile;

import com.chenweikeng.imf.nra.config.ConfigSetting;
import com.chenweikeng.imf.nra.config.ModConfig;

public class HistoryEntry {
  public long replacedAt;
  public ConfigSetting data;

  public HistoryEntry() {}

  public HistoryEntry(long replacedAt, ConfigSetting data) {
    this.replacedAt = replacedAt;
    this.data = data;
  }

  public static HistoryEntry fromCurrentConfig() {
    return new HistoryEntry(System.currentTimeMillis(), ModConfig.currentSetting.copy());
  }
}
