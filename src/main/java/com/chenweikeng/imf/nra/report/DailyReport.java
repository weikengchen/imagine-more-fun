package com.chenweikeng.imf.nra.report;

import com.chenweikeng.imf.nra.ride.RideName;
import java.util.List;
import java.util.Map;

public class DailyReport {
  public final String date;
  public final int totalRides;
  public final long totalRideTimeSeconds;
  public final long totalOnlineSeconds;
  public final List<RideDelta> rideDeltas;
  public final String mvpRide;
  public final int mvpRideCount;
  public final String speedDemonRide;
  public final List<MilestoneReached> newMilestones;
  public final Grade grade;
  public final String title;
  public final Integer previousDayRides;

  /** The actual date whose snapshot was used as the baseline (may be older than date-1). */
  public final String previousRideDate;

  public final boolean isFirstDay;
  public final boolean isLive;

  // Pin activity stats
  public final int pinHoarderTrades;
  public final int pinBoxesOpened;
  public final int newMintPinsAdded;

  // Food consumption stats (sorted by item name)
  public final Map<String, Integer> foodConsumed;

  public DailyReport(
      String date,
      int totalRides,
      long totalRideTimeSeconds,
      long totalOnlineSeconds,
      List<RideDelta> rideDeltas,
      String mvpRide,
      int mvpRideCount,
      String speedDemonRide,
      List<MilestoneReached> newMilestones,
      Grade grade,
      String title,
      Integer previousDayRides,
      String previousRideDate,
      boolean isFirstDay,
      boolean isLive,
      int pinHoarderTrades,
      int pinBoxesOpened,
      int newMintPinsAdded,
      Map<String, Integer> foodConsumed) {
    this.date = date;
    this.totalRides = totalRides;
    this.totalRideTimeSeconds = totalRideTimeSeconds;
    this.totalOnlineSeconds = totalOnlineSeconds;
    this.rideDeltas = rideDeltas;
    this.mvpRide = mvpRide;
    this.mvpRideCount = mvpRideCount;
    this.speedDemonRide = speedDemonRide;
    this.newMilestones = newMilestones;
    this.grade = grade;
    this.title = title;
    this.previousDayRides = previousDayRides;
    this.previousRideDate = previousRideDate;
    this.isFirstDay = isFirstDay;
    this.isLive = isLive;
    this.pinHoarderTrades = pinHoarderTrades;
    this.pinBoxesOpened = pinBoxesOpened;
    this.newMintPinsAdded = newMintPinsAdded;
    this.foodConsumed = foodConsumed;
  }

  public static class RideDelta {
    public final RideName ride;
    public final int countIncrease;
    public final int newTotal;
    public final long timeContributedSeconds;

    public RideDelta(RideName ride, int countIncrease, int newTotal, long timeContributedSeconds) {
      this.ride = ride;
      this.countIncrease = countIncrease;
      this.newTotal = newTotal;
      this.timeContributedSeconds = timeContributedSeconds;
    }
  }

  public static class MilestoneReached {
    public final RideName ride;
    public final int milestone;

    public MilestoneReached(RideName ride, int milestone) {
      this.ride = ride;
      this.milestone = milestone;
    }
  }

  public enum Grade {
    S("S", 0xFFFFD700, "Legendary Grinder"),
    A("A", 0xFF4CAF50, "Ride Warrior"),
    B("B", 0xFF42A5F5, "Steady Rider"),
    C("C", 0xFFFFA726, "Casual Cruiser"),
    D("D", 0xFFEF5350, "Taking It Easy");

    public final String label;
    public final int color;
    public final String title;

    Grade(String label, int color, String title) {
      this.label = label;
      this.color = color;
      this.title = title;
    }

    public static Grade fromRideCount(int totalRides) {
      if (totalRides >= 100) return S;
      if (totalRides >= 50) return A;
      if (totalRides >= 25) return B;
      if (totalRides >= 10) return C;
      return D;
    }

    public static Grade fromRideTime(long rideTimeSeconds) {
      if (rideTimeSeconds >= 8 * 3600) return S; // 8+ hours
      if (rideTimeSeconds >= 4 * 3600) return A; // 4+ hours
      if (rideTimeSeconds >= 2 * 3600) return B; // 2+ hours
      if (rideTimeSeconds >= 1 * 3600) return C; // 1+ hour
      return D;
    }

    public static Grade bestOf(int totalRides, long rideTimeSeconds) {
      Grade byRides = fromRideCount(totalRides);
      Grade byTime = fromRideTime(rideTimeSeconds);
      return byRides.ordinal() <= byTime.ordinal() ? byRides : byTime;
    }
  }
}
