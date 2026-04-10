package com.chenweikeng.imf.nra.tracker;

import com.chenweikeng.imf.nra.GameState;
import com.chenweikeng.imf.nra.Timing;
import com.chenweikeng.imf.nra.ride.CurrentRideHolder;
import com.chenweikeng.imf.nra.ride.RideName;
import net.minecraft.client.Minecraft;

public class RideStateTracker {
  private long lastRideTick = -1;
  private long lastVehicleTick = -1;
  private RideName previousRide = null;
  private boolean lincolnSuppressionActive = false;

  public void trackRideCompletion(long currentTick) {
    RideName currentRide = CurrentRideHolder.getCurrentRide();

    if (currentRide != null || previousRide != null) {
      lastRideTick = currentTick;
    }

    if (previousRide == RideName.GREAT_MOMENTS_WITH_MR_LINCOLN && currentRide == null) {
      lincolnSuppressionActive = false;
    }

    previousRide = currentRide;
  }

  public void trackVehicleState(Minecraft client, long currentTick) {
    if (client.player != null && GameState.getInstance().isValidPassenger(client.player)) {
      lastVehicleTick = currentTick;
    }
  }

  public boolean hasRidenRecently(long currentTick) {
    if (lastRideTick < 0) {
      return false;
    }

    long ticksSinceLastRide = currentTick - lastRideTick;
    return ticksSinceLastRide < Timing.RIDE_COMPLETION_SUPPRESSION_TICKS;
  }

  public boolean hasVehicleRecently(long currentTick) {
    if (lastVehicleTick < 0) {
      return false;
    }

    long ticksSinceLastVehicle = currentTick - lastVehicleTick;
    return ticksSinceLastVehicle < Timing.VEHICLE_SUPPRESSION_TICKS;
  }

  public boolean isLincolnSuppressionActive() {
    return lincolnSuppressionActive;
  }

  public void setLincolnSuppressionActive(boolean active) {
    this.lincolnSuppressionActive = active;
  }

  public void reset() {
    lastRideTick = -1;
    lastVehicleTick = -1;
    previousRide = null;
    lincolnSuppressionActive = false;
  }
}
