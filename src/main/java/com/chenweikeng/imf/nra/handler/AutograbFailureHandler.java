package com.chenweikeng.imf.nra.handler;

import com.chenweikeng.imf.nra.GameState;
import com.chenweikeng.imf.nra.ServerState;
import com.chenweikeng.imf.nra.ride.AutograbHolder;
import com.chenweikeng.imf.nra.ride.RideName;
import com.chenweikeng.imf.nra.tracker.PlayerMovementTracker;
import net.minecraft.client.Minecraft;

public class AutograbFailureHandler {
  private static final int AUTOGRAB_TIMEOUT_TICKS = 600;
  private static final int AUTOGRAB_DLRR_TIMEOUT_TICKS = 1200;

  private long autograbRegionEntryTick = -1;
  private RideName currentAutograbRegion = null;
  private boolean autograbFailureAlertActive = false;

  public boolean track(Minecraft client, long currentTick, PlayerMovementTracker movementTracker) {
    if (!ServerState.isImagineFunServer()) {
      return false;
    }
    if (client.player == null) {
      return false;
    }

    boolean isPassenger = GameState.getInstance().isValidPassenger(client.player);
    RideName autograbRide = AutograbHolder.getRideAtLocation(client);

    if (autograbRide != null && !isPassenger) {
      if (currentAutograbRegion != autograbRide) {
        currentAutograbRegion = autograbRide;
        autograbRegionEntryTick = currentTick;
        autograbFailureAlertActive = false;
      }

      int timeoutTicks =
          autograbRide == RideName.DISNEYLAND_RAILROAD
              ? AUTOGRAB_DLRR_TIMEOUT_TICKS
              : AUTOGRAB_TIMEOUT_TICKS;

      if (autograbRegionEntryTick >= 0 && currentTick - autograbRegionEntryTick >= timeoutTicks) {
        autograbFailureAlertActive = true;
      }
    } else {
      currentAutograbRegion = null;
      autograbRegionEntryTick = -1;
      autograbFailureAlertActive = false;
    }

    return autograbFailureAlertActive && !movementTracker.hasPlayerMovedRecently(currentTick);
  }

  public void reset() {
    autograbRegionEntryTick = -1;
    currentAutograbRegion = null;
    autograbFailureAlertActive = false;
  }
}
