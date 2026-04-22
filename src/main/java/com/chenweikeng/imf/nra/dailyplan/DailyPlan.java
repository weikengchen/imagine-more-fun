package com.chenweikeng.imf.nra.dailyplan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyPlan {
  /** ISO-8601 local date the plan was generated for (e.g. "2026-04-23"). */
  public String date;

  public long generatedAtEpochMs;

  /** Snapshot of each ride's count at generation time (keyed by ride match name). Prospective */
  /* credit counts deltas against this baseline. */
  public Map<String, Integer> snapshotCounts = new HashMap<>();

  public List<DailyPlanNode> nodes = new ArrayList<>();

  public DailyPlan() {}
}
