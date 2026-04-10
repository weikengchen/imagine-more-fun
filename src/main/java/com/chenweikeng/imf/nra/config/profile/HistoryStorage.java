package com.chenweikeng.imf.nra.config.profile;

import com.chenweikeng.imf.ImfStorage;
import com.chenweikeng.imf.nra.NotRidingAlertClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class HistoryStorage {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Path STORAGE_PATH = ImfStorage.nraHistory();
  private static final int CURRENT_VERSION = 1;

  private HistoryStorage() {}

  public static class HistoryStorageData {
    public int version;
    public List<HistoryEntry> entries;

    public HistoryStorageData() {
      this.version = CURRENT_VERSION;
      this.entries = new ArrayList<>();
    }

    public HistoryStorageData(int version, List<HistoryEntry> entries) {
      this.version = version;
      this.entries = entries != null ? entries : new ArrayList<>();
    }
  }

  public static HistoryStorageData load() {
    File storageFile = STORAGE_PATH.toFile();
    if (!storageFile.exists()) {
      return new HistoryStorageData();
    }

    try (FileReader reader = new FileReader(storageFile)) {
      HistoryStorageData data = GSON.fromJson(reader, HistoryStorageData.class);
      if (data == null) {
        return new HistoryStorageData();
      }
      if (data.entries == null) {
        data.entries = new ArrayList<>();
      }
      data.entries.removeIf(e -> e == null || e.data == null);
      return data;
    } catch (Exception e) {
      NotRidingAlertClient.LOGGER.warn(
          "Failed to load history storage, starting fresh: {}", e.getMessage());
      return new HistoryStorageData();
    }
  }

  public static void save(HistoryStorageData data) {
    if (data == null) {
      return;
    }
    try {
      Files.createDirectories(STORAGE_PATH.getParent());
      try (FileWriter writer = new FileWriter(STORAGE_PATH.toFile())) {
        GSON.toJson(data, writer);
      }
    } catch (IOException e) {
      NotRidingAlertClient.LOGGER.error("Failed to save history storage: {}", e.getMessage());
    }
  }
}
