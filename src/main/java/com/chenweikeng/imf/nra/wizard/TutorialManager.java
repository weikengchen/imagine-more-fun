package com.chenweikeng.imf.nra.wizard;

import com.chenweikeng.imf.ImfStorage;
import com.chenweikeng.imf.nra.NotRidingAlertClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public class TutorialManager {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Path CONFIG_PATH = ImfStorage.nraTutorial();

  private static TutorialManager instance;

  private TutorialState state = TutorialState.NOT_STARTED;
  private boolean completed = false;
  private String completedVersion = null;

  private TutorialManager() {
    load();
  }

  public static TutorialManager getInstance() {
    if (instance == null) {
      instance = new TutorialManager();
    }
    return instance;
  }

  public static String getCurrentModVersion() {
    return FabricLoader.getInstance()
        .getModContainer(NotRidingAlertClient.MOD_ID)
        .map(container -> container.getMetadata().getVersion().getFriendlyString())
        .orElse("unknown");
  }

  public boolean shouldStartTutorial() {
    return state == TutorialState.NOT_STARTED && NotRidingAlertClient.isImagineFunServer();
  }

  public boolean isTutorialActive() {
    return state.isActive();
  }

  public boolean isCompletedForCurrentVersion() {
    if (!completed) {
      return false;
    }
    String currentVersion = getCurrentModVersion();
    return currentVersion.equals(completedVersion);
  }

  public TutorialState getState() {
    return state;
  }

  public int getCurrentPageIndex() {
    return state.getPageIndex();
  }

  public void advanceToNextPage() {
    if (state != TutorialState.FINISHED) {
      state = state.getNext();
      if (state == TutorialState.FINISHED) {
        completed = true;
        save();
      }
    }
  }

  public void goToPage(int pageIndex) {
    if (!NotRidingAlertClient.isImagineFunServer()) {
      return;
    }
    TutorialState newState = TutorialState.fromPageIndex(pageIndex);
    if (newState.isActive()) {
      state = newState;
      save();
    }
  }

  public void finishTutorial() {
    state = TutorialState.FINISHED;
    completed = true;
    completedVersion = getCurrentModVersion();
    save();
  }

  public void resetTutorial() {
    state = TutorialState.NOT_STARTED;
  }

  public void load() {
    File configFile = CONFIG_PATH.toFile();
    if (!configFile.exists()) {
      return;
    }

    try (FileReader reader = new FileReader(configFile)) {
      TutorialData data = GSON.fromJson(reader, TutorialData.class);
      if (data != null) {
        this.completed = data.completed;
        this.completedVersion = data.completedVersion;
      }
    } catch (IOException e) {
      NotRidingAlertClient.LOGGER.warn("Failed to load tutorial state", e);
    }
  }

  public void save() {
    try {
      File configFile = CONFIG_PATH.toFile();
      configFile.getParentFile().mkdirs();

      TutorialData data = new TutorialData();
      data.completed = this.completed;
      data.completedVersion = this.completedVersion;

      try (FileWriter writer = new FileWriter(configFile)) {
        GSON.toJson(data, writer);
      }
    } catch (IOException e) {
      NotRidingAlertClient.LOGGER.warn("Failed to save tutorial state", e);
    }
  }

  private static class TutorialData {
    boolean completed;
    String completedVersion;
  }
}
