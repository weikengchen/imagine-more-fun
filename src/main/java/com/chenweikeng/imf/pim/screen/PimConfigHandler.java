package com.chenweikeng.imf.pim.screen;

import com.chenweikeng.imf.pim.PimClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class PimConfigHandler {
  private static PimConfigHandler instance;
  private final Gson gson;
  private final File configFile;
  private double fmvDiscount = 1.0;

  private PimConfigHandler() {
    this.gson = new GsonBuilder().setPrettyPrinting().create();
    this.configFile = new File("config/pim_config.json");
    load();
  }

  public static PimConfigHandler getInstance() {
    if (instance == null) {
      instance = new PimConfigHandler();
    }
    return instance;
  }

  public double getFmvDiscount() {
    return fmvDiscount;
  }

  public void setFmvDiscount(double discount) {
    this.fmvDiscount = discount;
    save();
  }

  private void save() {
    if (configFile.getParentFile() != null && !configFile.getParentFile().exists()) {
      configFile.getParentFile().mkdirs();
    }

    try (FileWriter writer = new FileWriter(configFile)) {
      ConfigData data = new ConfigData();
      data.fmvDiscount = fmvDiscount;
      gson.toJson(data, writer);
    } catch (IOException e) {
      PimClient.LOGGER.error("[Pim] Failed to save config", e);
    }
  }

  private void load() {
    if (!configFile.exists()) {
      save();
      return;
    }

    try (FileReader reader = new FileReader(configFile)) {
      ConfigData data = gson.fromJson(reader, ConfigData.class);
      if (data != null && data.fmvDiscount > 0) {
        fmvDiscount = data.fmvDiscount;
      }
    } catch (IOException e) {
      PimClient.LOGGER.error("[Pim] Failed to load config", e);
    }
  }

  private static class ConfigData {
    public double fmvDiscount = 1.0;
  }
}
