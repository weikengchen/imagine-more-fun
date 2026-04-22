package com.chenweikeng.imf.nra.dailyplan;

/**
 * One node in the daily ride plan. Stage 1 is linear-only, so every node is a single ride with a
 * target count k. {@code completed} is reserved for Stage 2 (auto-completion) — always false in
 * Stage 1.
 */
public class DailyPlanNode {
  /** The ride's match name (see {@code RideName.toMatchString()}). */
  public String ride;

  /** Target completion count for this node. */
  public int k;

  public boolean completed;

  public DailyPlanNode() {}

  public DailyPlanNode(String ride, int k) {
    this.ride = ride;
    this.k = k;
    this.completed = false;
  }
}
