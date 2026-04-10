package com.chenweikeng.imf.nra.ride;

/**
 * Holds the last ride the player was on (from the scoreboard "Current Ride"). Persists even when
 * the current ride disappears (e.g. after getting off).
 */
public class LastRideHolder {
  private static RideName lastRide = null;

  public static RideName getLastRide() {
    return lastRide;
  }

  public static void setLastRide(RideName ride) {
    lastRide = ride;
  }
}
