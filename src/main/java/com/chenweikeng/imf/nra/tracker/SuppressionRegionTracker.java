package com.chenweikeng.imf.nra.tracker;

import net.minecraft.client.Minecraft;

public class SuppressionRegionTracker {
  private static final double ROTR_SUPPRESSION_X = 674.0;
  private static final double ROTR_SUPPRESSION_Y = 65.0;
  private static final double ROTR_SUPPRESSION_Z = 984.0;
  private static final double ROTR_SUPPRESSION_RADIUS = 4.0;

  private static final int LINCOLN_SUPPRESSION_X_MIN = -132;
  private static final int LINCOLN_SUPPRESSION_X_MAX = -106;
  private static final int LINCOLN_SUPPRESSION_Z_MIN = 11;
  private static final int LINCOLN_SUPPRESSION_Z_MAX = 52;

  private boolean wasInLincolnRegion = false;

  public void trackLincolnRegionEntryExit(Minecraft client, RideStateTracker rideStateTracker) {
    boolean currentlyInRegion = isInLincolnSuppressionArea(client);

    if (currentlyInRegion && !wasInLincolnRegion) {
      rideStateTracker.setLincolnSuppressionActive(true);
    } else if (!currentlyInRegion && wasInLincolnRegion) {
      rideStateTracker.setLincolnSuppressionActive(false);
    }

    wasInLincolnRegion = currentlyInRegion;
  }

  public boolean isInROTRExceptionArea(Minecraft client) {
    if (client.player == null) {
      return false;
    }

    double dx = client.player.getX() - ROTR_SUPPRESSION_X;
    double dy = client.player.getY() - ROTR_SUPPRESSION_Y;
    double dz = client.player.getZ() - ROTR_SUPPRESSION_Z;
    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

    return distance <= ROTR_SUPPRESSION_RADIUS;
  }

  public boolean isInLincolnSuppressionArea(Minecraft client) {
    if (client.player == null) {
      return false;
    }

    double x = client.player.getX();
    double z = client.player.getZ();

    return x >= LINCOLN_SUPPRESSION_X_MIN
        && x <= LINCOLN_SUPPRESSION_X_MAX
        && z >= LINCOLN_SUPPRESSION_Z_MIN
        && z <= LINCOLN_SUPPRESSION_Z_MAX;
  }

  public void reset() {
    wasInLincolnRegion = false;
  }
}
