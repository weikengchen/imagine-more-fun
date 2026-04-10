package com.chenweikeng.imf.nra.tracker;

public class OtherPlayerStatsTracker {
  private static final OtherPlayerStatsTracker INSTANCE = new OtherPlayerStatsTracker();

  private boolean rideStatsActive = false;

  private OtherPlayerStatsTracker() {}

  public static OtherPlayerStatsTracker getInstance() {
    return INSTANCE;
  }

  public void setRideStatsActive(boolean active) {
    this.rideStatsActive = active;
  }

  public boolean isRideStatsActive() {
    return rideStatsActive;
  }

  public void reset() {
    rideStatsActive = false;
  }
}
