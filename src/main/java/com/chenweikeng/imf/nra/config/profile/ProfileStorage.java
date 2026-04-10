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

public final class ProfileStorage {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Path STORAGE_PATH = ImfStorage.nraProfiles();
  private static final int CURRENT_VERSION = 1;

  private ProfileStorage() {}

  public static class ProfileStorageData {
    public int version;
    public List<StoredProfile> profiles;

    public ProfileStorageData() {
      this.version = CURRENT_VERSION;
      this.profiles = new ArrayList<>();
    }

    public ProfileStorageData(int version, List<StoredProfile> profiles) {
      this.version = version;
      this.profiles = profiles != null ? profiles : new ArrayList<>();
    }

    public static ProfileStorageData createEmpty() {
      return new ProfileStorageData();
    }

    public static ProfileStorageData withBuiltIns() {
      ProfileStorageData data = new ProfileStorageData();
      data.profiles.addAll(BuiltInProfiles.all());
      return data;
    }
  }

  public static ProfileStorageData load() {
    File storageFile = STORAGE_PATH.toFile();
    if (!storageFile.exists()) {
      NotRidingAlertClient.LOGGER.info("Profile storage file not found, creating with built-ins");
      return ProfileStorageData.withBuiltIns();
    }

    try (FileReader reader = new FileReader(storageFile)) {
      ProfileStorageData data = GSON.fromJson(reader, ProfileStorageData.class);
      if (data == null) {
        NotRidingAlertClient.LOGGER.warn("Profile storage file is empty, creating with built-ins");
        return ProfileStorageData.withBuiltIns();
      }
      if (data.profiles == null) {
        data.profiles = new ArrayList<>();
      }
      List<StoredProfile> validProfiles = validateAndFilterProfiles(data.profiles);
      data.profiles = validProfiles;
      ensureBuiltInsExist(data);
      return data;
    } catch (Exception e) {
      NotRidingAlertClient.LOGGER.warn(
          "Failed to load profile storage, recreating with built-ins: {}", e.getMessage());
      return ProfileStorageData.withBuiltIns();
    }
  }

  private static List<StoredProfile> validateAndFilterProfiles(List<StoredProfile> profiles) {
    List<StoredProfile> valid = new ArrayList<>();
    for (StoredProfile profile : profiles) {
      if (isProfileValid(profile)) {
        valid.add(profile);
      } else {
        NotRidingAlertClient.LOGGER.warn(
            "Skipping corrupted profile: {} (id={})", profile.name, profile.id);
      }
    }
    return valid;
  }

  private static boolean isProfileValid(StoredProfile profile) {
    if (profile == null) return false;
    if (profile.id == null || profile.id.isBlank()) return false;
    if (profile.name == null || profile.name.isBlank()) return false;
    if (profile.data == null) return false;
    return true;
  }

  public static void save(ProfileStorageData data) {
    if (data == null) {
      return;
    }
    try {
      Files.createDirectories(STORAGE_PATH.getParent());
      try (FileWriter writer = new FileWriter(STORAGE_PATH.toFile())) {
        GSON.toJson(data, writer);
      }
    } catch (IOException e) {
      NotRidingAlertClient.LOGGER.error("Failed to save profile storage: {}", e.getMessage());
    }
  }

  private static void ensureBuiltInsExist(ProfileStorageData data) {
    List<StoredProfile> builtIns = BuiltInProfiles.all();
    for (StoredProfile builtIn : builtIns) {
      boolean exists = data.profiles.stream().anyMatch(p -> p.id.equals(builtIn.id));
      if (!exists) {
        data.profiles.add(builtIn.copy());
      }
    }
  }
}
