package com.chenweikeng.imf.nra.compat;

import net.fabricmc.loader.api.FabricLoader;

public final class MonkeycraftCompat {
  private MonkeycraftCompat() {}

  private static boolean initialized = false;

  public static boolean isAvailable() {
    return FabricLoader.getInstance().isModLoaded("monkeycraft");
  }

  public static void init() {
    if (initialized) {
      return;
    }
    if (!isAvailable()) {
      return;
    }
    MonkeycraftCompatImpl.init();
    initialized = true;
  }

  public static boolean isClientConnected() {
    if (!isAvailable()) {
      return false;
    }
    return MonkeycraftCompatImpl.isClientConnected();
  }

  public static boolean isServerStarted() {
    if (!isAvailable()) {
      return false;
    }
    return MonkeycraftCompatImpl.isServerStarted();
  }

  public static boolean isHibernating() {
    if (!isAvailable()) {
      return false;
    }
    return MonkeycraftCompatImpl.isHibernating();
  }

  public static void setTimedNotification(
      Long fireAtEpochMs, String title, String body, boolean sound, String countDownText) {
    if (!isAvailable()) {
      return;
    }
    MonkeycraftCompatImpl.setTimedNotification(fireAtEpochMs, title, body, sound, countDownText);
  }

  public static void cancelTimedNotification() {
    if (!isAvailable()) {
      return;
    }
    MonkeycraftCompatImpl.cancelTimedNotification();
  }

  public static void sendImmediateNotification(String title, String body, boolean sound) {
    if (!isAvailable()) {
      return;
    }
    MonkeycraftCompatImpl.sendImmediateNotification(title, body, sound);
  }

  public static void startHibernation(String message) {
    if (!isAvailable()) {
      return;
    }
    MonkeycraftCompatImpl.startHibernation(message);
  }

  public static void setHibernationMessage(String message) {
    if (!isAvailable()) {
      return;
    }
    MonkeycraftCompatImpl.setHibernationMessage(message);
  }

  public static void endHibernation() {
    if (!isAvailable()) {
      return;
    }
    MonkeycraftCompatImpl.endHibernation();
  }
}
