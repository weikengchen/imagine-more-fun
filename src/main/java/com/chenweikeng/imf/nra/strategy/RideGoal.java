package com.chenweikeng.imf.nra.strategy;

import com.chenweikeng.imf.nra.ride.RideName;

/** Represents a goal for a specific ride with information about progress and time needed. */
public class RideGoal {
  private final RideName ride;
  private final int currentCount;
  private final int nextGoal;
  private final int nextGoalRidesNeeded;
  private final long nextGoalTimeNeeded; // Time in seconds needed to reach the goal
  private final int maxGoal;
  private final int maxRidesNeeded;
  private final long maxTimeNeeded;

  public RideGoal(
      RideName ride,
      int currentCount,
      int nextGoal,
      int nextGoalRidesNeeded,
      long nextGoalTimeNeeded,
      int maxGoal,
      int maxRidesNeeded,
      long maxTimeNeeded) {
    this.ride = ride;
    this.currentCount = currentCount;
    this.nextGoal = nextGoal;
    this.nextGoalRidesNeeded = nextGoalRidesNeeded;
    this.nextGoalTimeNeeded = nextGoalTimeNeeded;
    this.maxGoal = maxGoal;
    this.maxRidesNeeded = maxRidesNeeded;
    this.maxTimeNeeded = maxTimeNeeded;
  }

  public RideName getRide() {
    return ride;
  }

  public int getCurrentCount() {
    return currentCount;
  }

  public int getNextGoal() {
    return nextGoal;
  }

  public int getNextGoalRidesNeeded() {
    return nextGoalRidesNeeded;
  }

  public long getNextGoalTimeNeeded() {
    return nextGoalTimeNeeded;
  }

  public int getMaxGoal() {
    return maxGoal;
  }

  public int getMaxRidesNeeded() {
    return maxRidesNeeded;
  }

  public long getMaxTimeNeeded() {
    return maxTimeNeeded;
  }

  public String nextToString() {
    return String.format(
        "%s - %d rides needed, %s",
        ride.getDisplayName(), nextGoalRidesNeeded, formatTime(nextGoalTimeNeeded));
  }

  public String maxToString() {
    return String.format(
        "%s - %d rides needed, %s",
        ride.getDisplayName(), maxRidesNeeded, formatTime(maxTimeNeeded));
  }

  private String formatTime(long seconds) {
    if (seconds < 60) {
      return seconds + "s";
    } else if (seconds < 3600) {
      long minutes = seconds / 60;
      long remainingSeconds = seconds % 60;
      if (remainingSeconds == 0) {
        return minutes + "m";
      }
      return minutes + "m " + remainingSeconds + "s";
    } else {
      long hours = seconds / 3600;
      long remainingMinutes = (seconds % 3600) / 60;
      if (remainingMinutes == 0) {
        return hours + "h";
      }
      return hours + "h " + remainingMinutes + "m";
    }
  }
}
