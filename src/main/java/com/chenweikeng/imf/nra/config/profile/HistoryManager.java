package com.chenweikeng.imf.nra.config.profile;

import com.chenweikeng.imf.nra.config.ConfigSetting;
import com.chenweikeng.imf.nra.config.ModConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HistoryManager {
  private static final long MAX_AGE_MILLIS = 30L * 24 * 60 * 60 * 1000;

  private static List<HistoryEntry> entries = new ArrayList<>();

  private HistoryManager() {}

  public static void load() {
    HistoryStorage.HistoryStorageData data = HistoryStorage.load();
    entries = data.entries != null ? data.entries : new ArrayList<>();
    if (pruneExpired()) {
      save();
    }
  }

  public static void save() {
    HistoryStorage.HistoryStorageData data = new HistoryStorage.HistoryStorageData(1, entries);
    HistoryStorage.save(data);
  }

  public static List<HistoryEntry> getAll() {
    List<HistoryEntry> sorted = new ArrayList<>(entries);
    sorted.sort(Comparator.comparingLong((HistoryEntry e) -> e.replacedAt).reversed());
    return sorted;
  }

  public static void backupIfNeeded() {
    ConfigSetting current = ModConfig.currentSetting;
    if (current == null) return;

    // Check against defaults
    ConfigSetting defaults = new ConfigSetting();
    if (current.equals(defaults)) return;

    // Check against all saved profiles
    for (StoredProfile profile : ProfileManager.getAllProfiles()) {
      if (profile.data != null && current.equals(profile.data)) return;
    }

    // Check against all built-in profiles
    for (StoredProfile builtIn : BuiltInProfiles.all()) {
      if (current.equals(builtIn.data)) return;
    }

    // Check against most recent history entry (deduplication)
    if (!entries.isEmpty()) {
      List<HistoryEntry> sorted = getAll();
      HistoryEntry mostRecent = sorted.get(0);
      if (mostRecent.data != null && current.equals(mostRecent.data)) return;
    }

    // No match found — back up current settings
    entries.add(HistoryEntry.fromCurrentConfig());
    save();
  }

  public static void applyEntry(HistoryEntry entry) {
    if (entry == null || entry.data == null) return;
    backupIfNeeded();
    ModConfig.currentSetting = entry.data.copy();
    ModConfig.save();
  }

  private static boolean pruneExpired() {
    long cutoff = System.currentTimeMillis() - MAX_AGE_MILLIS;
    return entries.removeIf(e -> e.replacedAt < cutoff);
  }
}
