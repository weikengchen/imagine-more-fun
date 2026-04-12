package com.chenweikeng.imf.nra.report;

import com.chenweikeng.imf.nra.ride.RideCountManager;
import com.chenweikeng.imf.nra.ride.RideName;
import com.chenweikeng.imf.nra.session.SessionTracker;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DailyReportGenerator {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final int[] MILESTONE_THRESHOLDS = {1, 10, 100, 500, 1000, 5000, 10000};

  public static DailyReport generate(String date) {
    DailyRideSnapshot snapshots = DailyRideSnapshot.getInstance();
    DailyRideSnapshot.SnapshotEntry todaySnap = snapshots.getSnapshot(date);
    if (todaySnap == null) {
      return null;
    }

    // Walk back to the most recent day that has data, skipping gap days.
    String prevDate = snapshots.getMostRecentDateBefore(date);
    DailyRideSnapshot.SnapshotEntry prevSnap =
        prevDate != null ? snapshots.getSnapshot(prevDate) : null;
    boolean isFirstDay = prevSnap == null;
    Map<String, Integer> prevCounts = prevSnap != null ? prevSnap.rideCounts : new HashMap<>();

    List<DailyReport.RideDelta> rideDeltas = new ArrayList<>();
    List<DailyReport.MilestoneReached> milestones = new ArrayList<>();
    String mvpRide = null;
    int mvpCount = 0;
    String speedDemonRide = null;
    double bestRidesPerHour = 0;

    for (Map.Entry<String, Integer> entry : todaySnap.rideCounts.entrySet()) {
      String rideKey = entry.getKey();
      int todayCount = entry.getValue();
      int prevCount = prevCounts.getOrDefault(rideKey, 0);
      int increase = todayCount - prevCount;

      if (increase <= 0) {
        continue;
      }

      RideName ride = RideName.fromMatchString(rideKey);
      if (ride == RideName.UNKNOWN) {
        continue;
      }

      // Ride deltas, MVP, and speed demon only make sense when we have a previous day baseline
      if (!isFirstDay) {
        long timeContributed = (long) increase * ride.getRideTime();
        rideDeltas.add(new DailyReport.RideDelta(ride, increase, todayCount, timeContributed));

        if (increase > mvpCount) {
          mvpCount = increase;
          mvpRide = ride.getDisplayName();
        }

        if (todaySnap.totalOnlineSeconds > 0) {
          double ridesPerHour = (double) increase / todaySnap.totalOnlineSeconds * 3600;
          if (ridesPerHour > bestRidesPerHour) {
            bestRidesPerHour = ridesPerHour;
            speedDemonRide = ride.getDisplayName();
          }
        }
      }

      // Milestones are always computed (including first day)
      if (isFirstDay) {
        // On first day, only show the highest milestone each ride has reached
        int highestMilestone = 0;
        for (int milestone : MILESTONE_THRESHOLDS) {
          if (todayCount >= milestone) {
            highestMilestone = milestone;
          }
        }
        if (highestMilestone > 0) {
          milestones.add(new DailyReport.MilestoneReached(ride, highestMilestone));
        }
      } else {
        for (int milestone : MILESTONE_THRESHOLDS) {
          if (prevCount < milestone && todayCount >= milestone) {
            milestones.add(new DailyReport.MilestoneReached(ride, milestone));
          }
        }
      }
    }

    rideDeltas.sort(
        Comparator.comparingInt((DailyReport.RideDelta d) -> d.countIncrease).reversed());

    DailyReport.Grade grade =
        DailyReport.Grade.bestOf(todaySnap.ridesCompleted, todaySnap.totalRideTimeSeconds);

    Integer previousDayRides = prevSnap != null ? prevSnap.ridesCompleted : null;

    // Sort food consumption by item name
    Map<String, Integer> sortedFood = new TreeMap<>(todaySnap.foodConsumed);

    return new DailyReport(
        date,
        todaySnap.ridesCompleted,
        todaySnap.totalRideTimeSeconds,
        todaySnap.totalOnlineSeconds,
        rideDeltas,
        mvpRide,
        mvpCount,
        speedDemonRide,
        milestones,
        grade,
        grade.title,
        previousDayRides,
        prevDate,
        isFirstDay,
        false,
        todaySnap.pinHoarderTrades,
        todaySnap.pinBoxesOpened,
        todaySnap.newMintPinsAdded,
        sortedFood);
  }

  /** Generates a live preview report for today using current in-memory ride counts and session. */
  public static DailyReport generateLive() {
    SessionTracker session = SessionTracker.getInstance();
    if (!session.isActive()) {
      return null;
    }

    String todayDate = LocalDate.now().format(DATE_FORMAT);
    int ridesCompleted = session.getRidesToday();
    long rideTimeSeconds = session.getRideTimeToday();
    long onlineSeconds = session.getOnlineSeconds();

    // Walk back to the most recent day that has data, skipping gap days.
    DailyRideSnapshot snapshots = DailyRideSnapshot.getInstance();
    String prevDate = snapshots.getMostRecentDateBefore(todayDate);
    DailyRideSnapshot.SnapshotEntry prevSnap =
        prevDate != null ? snapshots.getSnapshot(prevDate) : null;
    boolean isFirstDay = prevSnap == null;
    Map<String, Integer> prevCounts = prevSnap != null ? prevSnap.rideCounts : new HashMap<>();

    // Current live ride counts
    Map<RideName, Integer> liveCounts = RideCountManager.getInstance().getAllRideCounts();

    List<DailyReport.RideDelta> rideDeltas = new ArrayList<>();
    List<DailyReport.MilestoneReached> milestones = new ArrayList<>();
    String mvpRide = null;
    int mvpCount = 0;
    String speedDemonRide = null;
    double bestRidesPerHour = 0;

    for (Map.Entry<RideName, Integer> entry : liveCounts.entrySet()) {
      RideName ride = entry.getKey();
      if (ride == RideName.UNKNOWN) {
        continue;
      }
      int todayCount = entry.getValue();
      int prevCount = prevCounts.getOrDefault(ride.toMatchString(), 0);
      int increase = todayCount - prevCount;

      if (increase <= 0) {
        continue;
      }

      if (!isFirstDay) {
        long timeContributed = (long) increase * ride.getRideTime();
        rideDeltas.add(new DailyReport.RideDelta(ride, increase, todayCount, timeContributed));

        if (increase > mvpCount) {
          mvpCount = increase;
          mvpRide = ride.getDisplayName();
        }

        if (onlineSeconds > 0) {
          double ridesPerHour = (double) increase / onlineSeconds * 3600;
          if (ridesPerHour > bestRidesPerHour) {
            bestRidesPerHour = ridesPerHour;
            speedDemonRide = ride.getDisplayName();
          }
        }
      }

      if (isFirstDay) {
        int highestMilestone = 0;
        for (int milestone : MILESTONE_THRESHOLDS) {
          if (todayCount >= milestone) {
            highestMilestone = milestone;
          }
        }
        if (highestMilestone > 0) {
          milestones.add(new DailyReport.MilestoneReached(ride, highestMilestone));
        }
      } else {
        for (int milestone : MILESTONE_THRESHOLDS) {
          if (prevCount < milestone && todayCount >= milestone) {
            milestones.add(new DailyReport.MilestoneReached(ride, milestone));
          }
        }
      }
    }

    rideDeltas.sort(
        Comparator.comparingInt((DailyReport.RideDelta d) -> d.countIncrease).reversed());

    DailyReport.Grade grade = DailyReport.Grade.bestOf(ridesCompleted, rideTimeSeconds);

    Integer previousDayRides = prevSnap != null ? prevSnap.ridesCompleted : null;

    // Sort food consumption by item name
    Map<String, Integer> sortedFood = new TreeMap<>(session.getFoodConsumed());

    return new DailyReport(
        todayDate,
        ridesCompleted,
        rideTimeSeconds,
        onlineSeconds,
        rideDeltas,
        mvpRide,
        mvpCount,
        speedDemonRide,
        milestones,
        grade,
        grade.title,
        previousDayRides,
        prevDate,
        isFirstDay,
        true,
        session.getPinHoarderTrades(),
        session.getPinBoxesOpened(),
        session.getNewMintPinsAdded(),
        sortedFood);
  }
}
