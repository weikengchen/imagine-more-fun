package com.chenweikeng.imf.nra;

import com.chenweikeng.imf.nra.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerState {
  private static final Logger LOGGER = LoggerFactory.getLogger(NotRidingAlertClient.MOD_ID);
  private static boolean isImagineFunServer = false;
  private static float savedMusicVolume = -1f;

  public static boolean isImagineFunServer() {
    return isImagineFunServer && ModConfig.currentSetting.globalEnable;
  }

  public static void onJoin(Minecraft client) {
    if (client.getCurrentServer() == null || client.getCurrentServer().ip == null) {
      isImagineFunServer = false;
      LOGGER.info("No server info available on join");
      return;
    }

    String serverIp = client.getCurrentServer().ip.toLowerCase();
    isImagineFunServer = serverIp.endsWith(".imaginefun.net");

    if (isImagineFunServer) {
      LOGGER.info("Joined ImagineFun.net server: {}", serverIp);
      muteMusicVolume(client);
    } else {
      LOGGER.info("Joined non-ImagineFun.net server: {}", serverIp);
    }
  }

  public static void onDisconnect() {
    LOGGER.info("Disconnected from server");
    restoreMusicVolume();
    isImagineFunServer = false;
  }

  private static void muteMusicVolume(Minecraft client) {
    float current = client.options.getSoundSourceVolume(SoundSource.MUSIC);
    if (current > 0f) {
      savedMusicVolume = current;
      client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0.0);
      LOGGER.info("Muted background music (was {})", savedMusicVolume);
    }
  }

  private static void restoreMusicVolume() {
    if (savedMusicVolume >= 0f) {
      Minecraft client = Minecraft.getInstance();
      client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set((double) savedMusicVolume);
      LOGGER.info("Restored background music volume to {}", savedMusicVolume);
      savedMusicVolume = -1f;
    }
  }
}
