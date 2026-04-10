package com.chenweikeng.imf.pim;

import com.chenweikeng.imf.pim.command.PimCommand;
import com.chenweikeng.imf.pim.tracker.BossBarTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PimClient implements ClientModInitializer {

  public static final String MOD_ID = "pim";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
  private static boolean isImagineFunServer = false;

  @Override
  public void onInitializeClient() {
    LOGGER.info("Pim would like to welcome you.");

    ClientPlayConnectionEvents.JOIN.register(
        (handler, sender, client) -> {
          onJoin(client);
        });

    ClientPlayConnectionEvents.DISCONNECT.register(
        (handler, client) -> {
          onDisconnect();
        });

    PimCommand.registerCommands();
    BossBarTracker.getInstance();
  }

  public static boolean isImagineFunServer() {
    return isImagineFunServer;
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
    } else {
      LOGGER.info("Joined non-ImagineFun.net server: {}", serverIp);
    }
  }

  public static void onDisconnect() {
    LOGGER.info("Disconnected from server");
    isImagineFunServer = false;
  }
}
