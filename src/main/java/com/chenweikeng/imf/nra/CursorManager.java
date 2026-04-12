package com.chenweikeng.imf.nra;

import com.chenweikeng.imf.nra.compat.MonkeycraftCompat;
import com.chenweikeng.imf.nra.config.CursorReleaseTiming;
import com.chenweikeng.imf.nra.config.ModConfig;
import com.chenweikeng.imf.nra.config.WindowMinimizeTiming;
import com.chenweikeng.imf.nra.handler.WindowMinimizeHandler;
import com.chenweikeng.imf.nra.ride.AutograbHolder;
import com.chenweikeng.imf.nra.ride.CurrentRideHolder;
import com.chenweikeng.imf.nra.ride.RideName;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class CursorManager {
  public static final Component DYNAMIC_FPS_COMPATIBILITY_MESSAGE =
      Component.literal(
          "§6✨ §e[IMF] §fFor compatibility with Dynamic FPS, the window will not be minimized when MonkeyCraft client is connected.");

  private boolean wasRiding = false;
  private boolean wasOnVehicle = false;
  private boolean wasPassenger = false;
  private boolean minimizedDuringAutograb = false;
  private boolean autograbFailureRestored = false;
  private long pendingZoneMinimizeTick = -1;
  private RideName previousAutograbRide = null;
  private long lastCanoeMessageTick = -Timing.CANOE_MESSAGE_COOLDOWN_TICKS;
  private long lastDynamicFpsMessageTick = -Timing.DYNAMIC_FPS_MESSAGE_COOLDOWN_TICKS;
  private final WindowMinimizeHandler windowMinimizeHandler = WindowMinimizeHandler.getInstance();

  public void tick(Minecraft client, boolean isPassenger, boolean isRiding, RideName autograbRide) {
    GameState state = GameState.getInstance();
    CursorReleaseTiming timing = ModConfig.currentSetting.cursorReleaseTiming;

    if (timing == CursorReleaseTiming.ON_ZONE_ENTRY && autograbRide != null && !isPassenger) {
      if (autograbRide != previousAutograbRide) {
        client.setScreen(null);
        if (client.mouseHandler.isMouseGrabbed()) {
          client.mouseHandler.releaseMouse();
          state.setAutomaticallyReleasedCursor(true);
          sendCanoeMessageIfNeeded(client, autograbRide);
        }
        previousAutograbRide = autograbRide;
      }
    } else {
      previousAutograbRide = null;
    }

    boolean isOnVehicle = isPassenger || CurrentRideHolder.getCurrentRide() != null;
    if (timing != CursorReleaseTiming.NONE) {
      boolean shouldReleaseOnThisTick =
          switch (timing) {
            case NONE -> false;
            case ON_ZONE_ENTRY -> !wasRiding && isRiding;
            case ON_VEHICLE_MOUNT -> !wasOnVehicle && isOnVehicle;
          };

      if (shouldReleaseOnThisTick) {
        client.mouseHandler.releaseMouse();
        state.setAutomaticallyReleasedCursor(true);
        RideName currentRide = CurrentRideHolder.getCurrentRide();
        if (currentRide == null) {
          currentRide = AutograbHolder.getRideAtLocation(client);
        }
        sendCanoeMessageIfNeeded(client, currentRide);
      }

      boolean shouldGrabOnThisTick =
          switch (timing) {
            case NONE -> false;
            case ON_ZONE_ENTRY -> wasRiding && !isRiding;
            case ON_VEHICLE_MOUNT -> wasOnVehicle && !isOnVehicle;
          };

      if (shouldGrabOnThisTick) {
        state.setAutomaticallyReleasedCursor(false);
        if (client.screen == null) {
          client.mouseHandler.grabMouse();
        }
      }

      boolean isCurrentlyRiding =
          switch (timing) {
            case NONE -> false;
            case ON_ZONE_ENTRY -> isRiding;
            case ON_VEHICLE_MOUNT -> isOnVehicle;
          };

      if ((isCurrentlyRiding || client.player.isPassenger())
          && client.mouseHandler.isRightPressed()
          && client.screen == null) {
        client.mouseHandler.releaseMouse();
      }
    }

    if (ModConfig.currentSetting.minimizeWindow != WindowMinimizeTiming.NONE) {
      WindowMinimizeTiming minimizeTiming = ModConfig.currentSetting.minimizeWindow;
      long currentTick = state.getAbsoluteTickCounter();

      // Arm the zone-entry minimize timer the tick the player first enters an
      // autograb zone. The actual minimize is deferred by
      // ZONE_ENTRY_MINIMIZE_DELAY_TICKS so the player has a moment to walk past
      // the risky border of the zone before the window drops.
      if (!wasRiding && isRiding && minimizeTiming == WindowMinimizeTiming.ON_ZONE_ENTRY) {
        pendingZoneMinimizeTick = currentTick;
      }
      // Cancel the pending minimize if the player leaves the zone before the
      // delay elapses.
      if (!isRiding) {
        pendingZoneMinimizeTick = -1;
      }

      boolean shouldMinimizeOnZoneEntry =
          pendingZoneMinimizeTick != -1
              && (currentTick - pendingZoneMinimizeTick) >= Timing.ZONE_ENTRY_MINIMIZE_DELAY_TICKS;
      boolean shouldMinimizeOnVehicleMount =
          !wasOnVehicle && isOnVehicle && !minimizedDuringAutograb;

      boolean shouldMinimizeOnThisTick =
          switch (minimizeTiming) {
            case NONE -> false;
            case ON_ZONE_ENTRY -> shouldMinimizeOnZoneEntry || shouldMinimizeOnVehicleMount;
            case ON_VEHICLE_MOUNT -> shouldMinimizeOnVehicleMount;
          };

      if (shouldMinimizeOnThisTick) {
        if (MonkeycraftCompat.isClientConnected()
            && FabricLoader.getInstance().isModLoaded("dynamic_fps")) {
          sendDynamicFpsMessageIfNeeded(client);
        } else {
          if (shouldMinimizeOnZoneEntry && minimizeTiming == WindowMinimizeTiming.ON_ZONE_ENTRY) {
            minimizedDuringAutograb = true;
            // Engage the rubber band so the player can't wander out of the
            // autograb zone while the window is minimized. Only active if the
            // user has the autograb region display enabled. Releases after 1s
            // from initial zone entry, or when the player mounts / autograb
            // fails.
            if (ModConfig.currentSetting.showAutograbRegions
                && pendingZoneMinimizeTick != -1
                && client.player != null) {
              state.armRubberBand(
                  client.player.getX(),
                  client.player.getY(),
                  client.player.getZ(),
                  pendingZoneMinimizeTick + 20);
            }
          }
          windowMinimizeHandler.minimizeWindow();
        }
        // Any minimize path that fires satisfies the pending zone minimize, so
        // clear it to avoid re-triggering after the delay elapses.
        pendingZoneMinimizeTick = -1;
      }

      // Rubber band bookkeeping: clear on expiry / mount / missing player.
      // While the band is active and the player is still inside the zone,
      // update the anchor so the next border crossing snaps back to the
      // player's latest in-zone position. The actual snap is performed by
      // LocalPlayerMixin at HEAD of sendPosition() earlier in the same tick.
      if (state.isRubberBandActive()) {
        if (isPassenger || currentTick > state.getRubberBandUntilTick() || client.player == null) {
          state.clearRubberBand();
        } else if (autograbRide != null) {
          state.updateRubberBandAnchor(
              client.player.getX(), client.player.getY(), client.player.getZ());
        }
      }

      if (!isRiding) {
        minimizedDuringAutograb = false;
      }

      boolean shouldRestoreOnThisTick =
          switch (minimizeTiming) {
            case NONE -> false;
            case ON_ZONE_ENTRY -> wasRiding && !isRiding;
            case ON_VEHICLE_MOUNT -> wasOnVehicle && !isOnVehicle;
          };

      if (shouldRestoreOnThisTick) {
        // Suppress pause-on-focus-loss for 10 ticks (500ms) to give GLFW time to
        // process the restore/focus chain before setWindowActive() kicks in.
        state.setWindowRestoreGrace(10);
        windowMinimizeHandler.restoreWindow();
      }
      if (wasRiding && !isRiding) {
        windowMinimizeHandler.requestAttention();
      }

      if (MonkeycraftCompat.isClientConnected()
          && FabricLoader.getInstance().isModLoaded("dynamic_fps")) {
        if (client.getWindow() != null) {
          long handle = client.getWindow().handle();
          boolean isMinimized =
              GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE;
          if (isMinimized) {
            windowMinimizeHandler.restoreWindow();
            sendDynamicFpsMessageIfNeeded(client);
          }
        }
      }
    }

    wasRiding = isRiding;
    wasOnVehicle = isOnVehicle;
    wasPassenger = isPassenger;
  }

  public boolean wasPassenger() {
    return wasPassenger;
  }

  public void clearAutograbFailureRestored() {
    autograbFailureRestored = false;
  }

  public void handleAutograbFailureRestore() {
    if (autograbFailureRestored) {
      return;
    }
    autograbFailureRestored = true;
    GameState.getInstance().clearRubberBand();
    if (ModConfig.currentSetting.minimizeWindow != WindowMinimizeTiming.NONE) {
      windowMinimizeHandler.restoreWindow();
      minimizedDuringAutograb = false;
    }
    windowMinimizeHandler.requestAttention();
  }

  private void sendCanoeMessageIfNeeded(Minecraft client, RideName ride) {
    if (client.player == null || ride != RideName.DAVY_CROCKETTS_EXPLORER_CANOES) {
      return;
    }

    GameState state = GameState.getInstance();
    if (state.getAbsoluteTickCounter() - lastCanoeMessageTick
        < Timing.CANOE_MESSAGE_COOLDOWN_TICKS) {
      return;
    }

    lastCanoeMessageTick = state.getAbsoluteTickCounter();

    Component message =
        Component.literal("§6✨ §e[IMF] §fPlease use §e§lLEFT click§r§f to ride canoes.");

    client.player.displayClientMessage(message, false);
  }

  private void sendDynamicFpsMessageIfNeeded(Minecraft client) {
    if (client.player == null) {
      return;
    }

    GameState state = GameState.getInstance();
    if (state.getAbsoluteTickCounter() - lastDynamicFpsMessageTick
        < Timing.DYNAMIC_FPS_MESSAGE_COOLDOWN_TICKS) {
      return;
    }

    lastDynamicFpsMessageTick = state.getAbsoluteTickCounter();

    client.player.displayClientMessage(DYNAMIC_FPS_COMPATIBILITY_MESSAGE, false);
  }

  public void reset() {
    wasRiding = false;
    wasOnVehicle = false;
    wasPassenger = false;
    minimizedDuringAutograb = false;
    autograbFailureRestored = false;
    pendingZoneMinimizeTick = -1;
    GameState.getInstance().clearRubberBand();
    previousAutograbRide = null;
    lastCanoeMessageTick = -Timing.CANOE_MESSAGE_COOLDOWN_TICKS;
    lastDynamicFpsMessageTick = -Timing.DYNAMIC_FPS_MESSAGE_COOLDOWN_TICKS;
  }
}
