package com.chenweikeng.imf.nra.handler;

import com.chenweikeng.imf.nra.compat.MonkeycraftCompat;
import com.chenweikeng.imf.nra.config.ModConfig;
import com.chenweikeng.imf.nra.config.SortingRules;
import com.chenweikeng.imf.nra.ride.CurrentRideHolder;
import com.chenweikeng.imf.nra.ride.RideName;
import com.chenweikeng.imf.nra.strategy.RideGoal;
import com.chenweikeng.imf.nra.strategy.StrategyCalculator;
import com.chenweikeng.imf.nra.util.TimeFormatUtil;
import net.minecraft.client.Minecraft;

public class HibernationHandler {
  private static final int CANCELLATION_DELAY_TICKS = 60;
  private static final int MESSAGE_UPDATE_INTERVAL_TICKS = 20;
  private static HibernationHandler instance;

  private RideName previousCurrentRide = null;
  private long rideEndTick = -1;
  private boolean pendingCancellation = false;
  private boolean hibernationActive = false;
  private boolean wasHibernationEligibleRide = false;

  private long rideStartEpochMs = -1;
  private int currentRideTimeSeconds = -1;
  private long lastMessageUpdateTick = -1;
  private boolean previousHibernationSetting = true;

  private HibernationHandler() {}

  public static HibernationHandler getInstance() {
    if (instance == null) {
      instance = new HibernationHandler();
    }
    return instance;
  }

  public void track(Minecraft client, long currentTick) {
    if (!MonkeycraftCompat.isAvailable()) {
      return;
    }

    boolean currentSetting = ModConfig.currentSetting.hibernationWhenRiding;

    if (previousHibernationSetting && !currentSetting && hibernationActive) {
      MonkeycraftCompat.endHibernation();
      hibernationActive = false;
    }

    RideName currentRide = CurrentRideHolder.getCurrentRide();

    if (currentRide != null && previousCurrentRide == null) {
      onRideStart(currentRide, currentSetting);
    } else if (currentRide == null && previousCurrentRide != null) {
      onRideEnd(currentTick);
    } else if (currentRide != null && currentRide != previousCurrentRide) {
      onRideEnd(currentTick);
      onRideStart(currentRide, currentSetting);
    }

    if (!previousHibernationSetting
        && currentSetting
        && currentRide != null
        && !hibernationActive
        && wasHibernationEligibleRide) {
      Integer progressPercent = CurrentRideHolder.getCurrentProgressPercent();
      int initialProgress = progressPercent != null ? progressPercent : 0;
      MonkeycraftCompat.startHibernation(buildHibernationMessage(currentRide, initialProgress));
      hibernationActive = true;
      lastMessageUpdateTick = currentTick;
    }

    if (pendingCancellation && currentTick - rideEndTick >= CANCELLATION_DELAY_TICKS) {
      executeCancellation();
    }

    if (hibernationActive && currentRideTimeSeconds > 0) {
      if (currentTick - lastMessageUpdateTick >= MESSAGE_UPDATE_INTERVAL_TICKS) {
        updateHibernationMessage();
        lastMessageUpdateTick = currentTick;
      }
    }

    previousCurrentRide = currentRide;
    previousHibernationSetting = currentSetting;
  }

  private void onRideStart(RideName ride, boolean hibernationEnabled) {
    if (ride == RideName.DAVY_CROCKETTS_EXPLORER_CANOES || ride == RideName.UNKNOWN) {
      wasHibernationEligibleRide = false;
      return;
    }

    pendingCancellation = false;
    wasHibernationEligibleRide = true;

    currentRideTimeSeconds = ride.getRideTime();
    lastMessageUpdateTick = -1;

    Integer progressPercent = CurrentRideHolder.getCurrentProgressPercent();
    if (progressPercent != null && progressPercent > 0 && currentRideTimeSeconds > 0) {
      int elapsedSeconds = (progressPercent * currentRideTimeSeconds) / 100;
      rideStartEpochMs = System.currentTimeMillis() - (elapsedSeconds * 1000L);
    } else {
      rideStartEpochMs = System.currentTimeMillis();
    }

    int initialProgress = progressPercent != null ? progressPercent : 0;

    if (hibernationEnabled) {
      MonkeycraftCompat.startHibernation(buildHibernationMessage(ride, initialProgress));
      hibernationActive = true;
    }

    scheduleRideCompletionNotification(ride);
  }

  private void onRideEnd(long currentTick) {
    if (hibernationActive) {
      MonkeycraftCompat.endHibernation();
      hibernationActive = false;
    }

    rideStartEpochMs = -1;
    currentRideTimeSeconds = -1;
    lastMessageUpdateTick = -1;

    if (wasHibernationEligibleRide) {
      rideEndTick = currentTick;
      pendingCancellation = true;
    }
  }

  private void updateHibernationMessage() {
    RideName currentRide = CurrentRideHolder.getCurrentRide();
    if (currentRide == null || currentRideTimeSeconds <= 0 || rideStartEpochMs < 0) {
      return;
    }

    long elapsedMs = System.currentTimeMillis() - rideStartEpochMs;
    int elapsedSeconds = (int) (elapsedMs / 1000);
    int progressPercent = Math.min(100, (elapsedSeconds * 100) / currentRideTimeSeconds);

    String message = buildHibernationMessage(currentRide, progressPercent);
    MonkeycraftCompat.setHibernationMessage(message);
  }

  private String buildHibernationMessage(RideName ride, int progressPercent) {
    StringBuilder sb = new StringBuilder();
    sb.append("Riding ").append(ride.getDisplayName());

    if (currentRideTimeSeconds > 0 && rideStartEpochMs > 0) {
      long elapsedMs = System.currentTimeMillis() - rideStartEpochMs;
      int elapsedSeconds = (int) (elapsedMs / 1000);
      int remainingSeconds = Math.max(0, currentRideTimeSeconds - elapsedSeconds);

      sb.append(" (").append(progressPercent).append("%)\n");
      sb.append(TimeFormatUtil.formatDuration(remainingSeconds)).append(" left");
    }

    return sb.toString();
  }

  private void scheduleRideCompletionNotification(RideName ride) {
    int rideTimeSeconds = ride.getRideTime();
    if (rideTimeSeconds >= 99999 || rideStartEpochMs < 0) {
      return;
    }

    long fireAtEpochMs = rideStartEpochMs + ((long) rideTimeSeconds * 1000);

    String title = "Ride finished";
    String body = buildNotificationBody(ride);

    MonkeycraftCompat.setTimedNotification(fireAtEpochMs, title, body, true, ride.getDisplayName());
  }

  private String buildNotificationBody(RideName ride) {
    RideGoal goal = StrategyCalculator.getGoalForRide(ride);
    String rideName = ride.getDisplayName();

    if (ModConfig.currentSetting.sortingRules == SortingRules.TOTAL_TIME_ASC
        || ModConfig.currentSetting.sortingRules == SortingRules.TOTAL_TIME_DESC) {
      if (goal == null || goal.getMaxRidesNeeded() <= 1) {
        return rideName + " has finished";
      }

      int ridesNeeded = goal.getMaxRidesNeeded();
      return rideName + " has finished (needs " + (ridesNeeded - 1) + " more rides)";
    } else {
      if (goal == null || goal.getNextGoalRidesNeeded() <= 1) {
        return rideName + " has finished";
      }

      int ridesNeeded = goal.getNextGoalRidesNeeded();
      return rideName + " has finished (needs " + (ridesNeeded - 1) + " more rides)";
    }
  }

  public void cancelPendingCancellation() {
    pendingCancellation = false;
  }

  private void executeCancellation() {
    MonkeycraftCompat.cancelTimedNotification();
    pendingCancellation = false;
  }

  public void reset() {
    if (hibernationActive) {
      MonkeycraftCompat.endHibernation();
    }
    previousCurrentRide = null;
    rideEndTick = -1;
    pendingCancellation = false;
    hibernationActive = false;
    wasHibernationEligibleRide = false;
    rideStartEpochMs = -1;
    currentRideTimeSeconds = -1;
    lastMessageUpdateTick = -1;
  }
}
