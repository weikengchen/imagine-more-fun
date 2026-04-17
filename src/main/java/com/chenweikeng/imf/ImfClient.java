package com.chenweikeng.imf;

import com.chenweikeng.imf.nra.NotRidingAlertClient;
import com.chenweikeng.imf.pim.PimClient;
import com.chenweikeng.imf.skincache.SkinCacheMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single client entrypoint for the merged ImagineMoreFun mod.
 *
 * <p>This class wires together three formerly-independent mods into one. Each sub-mod's original
 * initializer is still invoked unchanged — the order is NRA → PIM → SkinCache, but none of them has
 * a hard dependency on another, so the order is not load-bearing.
 *
 * <p>Storage migration runs exactly once before any sub-mod initializer touches the filesystem.
 */
public class ImfClient implements ClientModInitializer {

  public static final String MOD_ID = "imaginemorefun";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitializeClient() {
    LOGGER.info("ImagineMoreFun starting (NRA + PIM + SkinCache)");

    // Move old on-disk state into config/imaginemorefun/ before any sub-mod reads its files.
    try {
      ImfMigration.runOnce();
    } catch (RuntimeException e) {
      LOGGER.error("Storage migration failed; continuing anyway", e);
    }

    new NotRidingAlertClient().onInitializeClient();
    new PimClient().onInitializeClient();
    new SkinCacheMod().onInitializeClient();

    // Swap the macOS Dock icon for the ImagineFun logo while connected to ImagineFun, and restore
    // it on disconnect. Deferred to JOIN because applying it during init runs before the GLFW
    // window exists, and macOS resets the icon when the window comes up. No-op on other OSes.
    ClientPlayConnectionEvents.JOIN.register(
        (handler, sender, client) -> {
          if (client.getCurrentServer() == null || client.getCurrentServer().ip == null) {
            return;
          }
          if (client.getCurrentServer().ip.toLowerCase().endsWith(".imaginefun.net")) {
            MacosDockIconHandler.apply();
          }
        });
    ClientPlayConnectionEvents.DISCONNECT.register(
        (handler, client) -> MacosDockIconHandler.reset());
  }
}
