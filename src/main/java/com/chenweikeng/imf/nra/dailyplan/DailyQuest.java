package com.chenweikeng.imf.nra.dailyplan;

/**
 * One parsed daily quest read off the server's Daily Objectives chest GUI. Only ride-shaped goals
 * (verbs "Ride" and "Watch") are represented — other quest kinds (helping characters, etc.) are
 * intentionally dropped at parse time. Stored in {@link DailyQuestSnapshot} which is persisted to
 * disk so the plan can keep injecting quest layers across restarts.
 */
public class DailyQuest {
  /** Match name used by {@link com.chenweikeng.imf.nra.ride.RideName#fromMatchString}. */
  public String rideMatchName;

  /** Total laps the quest asks for. "Watch X" quests use 1. */
  public int target;

  /** What the server reported as already done at capture time (the "X" in "X / target"). */
  public int observedProgress;

  /** Reward in Kingdom Coins, parsed from "Reward: N Kingdom Coins". */
  public int rewardCoins;

  public DailyQuest() {}

  public DailyQuest(String rideMatchName, int target, int observedProgress, int rewardCoins) {
    this.rideMatchName = rideMatchName;
    this.target = target;
    this.observedProgress = observedProgress;
    this.rewardCoins = rewardCoins;
  }
}
