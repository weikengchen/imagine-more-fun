package com.chenweikeng.imf.nra.dailyplan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistent snapshot of the most-recent Daily Objectives reading. Captured by {@link
 * com.chenweikeng.imf.mixin.ImfDailyObjectivesScreenMixin} whenever the player opens that GUI;
 * survives restarts so the plan generator can keep using it without forcing the user to re-open the
 * screen each session.
 */
public class DailyQuestSnapshot {
  public long capturedAtEpochMs;

  /** ISO local-date the snapshot was captured on, used to expire stale snapshots. */
  public String capturedDate;

  public List<DailyQuest> quests = new ArrayList<>();

  /**
   * Local ride counts per match-name at the moment of capture. Used so plan-side baselines can be
   * pre-set such that {@code currentCount - baseline} reproduces the server-reported observed
   * progress at activation time.
   */
  public Map<String, Integer> rideCountsAtCapture = new HashMap<>();

  public DailyQuestSnapshot() {}
}
