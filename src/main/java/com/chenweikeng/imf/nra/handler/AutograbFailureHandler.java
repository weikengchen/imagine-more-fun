package com.chenweikeng.imf.nra.handler;

import com.chenweikeng.imf.nra.GameState;
import com.chenweikeng.imf.nra.ServerState;
import com.chenweikeng.imf.nra.ride.AutograbHolder;
import com.chenweikeng.imf.nra.ride.RideName;
import com.chenweikeng.imf.nra.tracker.PlayerMovementTracker;
import net.minecraft.client.Minecraft;

public class AutograbFailureHandler {
  private static final int AUTOGRAB_TIMEOUT_TICKS = 1200; // 60s at 20 TPS
  // Rise of the Resistance's boarding sequence legitimately takes longer than the 60s default, so
  // extend the failure threshold to 90s to avoid false alerts during normal loading.
  private static final int AUTOGRAB_TIMEOUT_TICKS_RISE = 1800; // 90s

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
          currentAutograbRegion == RideName.STAR_WARS_RISE_OF_THE_RESISTANCE
              ? AUTOGRAB_TIMEOUT_TICKS_RISE
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
