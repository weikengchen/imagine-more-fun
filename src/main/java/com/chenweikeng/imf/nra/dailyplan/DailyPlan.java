package com.chenweikeng.imf.nra.dailyplan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyPlan {
  /** ISO-8601 local date the plan was generated for (e.g. "2026-04-23"). */
  public String date;

  public long generatedAtEpochMs;

  /** Snapshot of each ride's count at generation time (keyed by ride match name). */
  public Map<String, Integer> snapshotCounts = new HashMap<>();

  /** Ordered list of layers. The plan auto-extends so the tail always has open work. */
  public List<DailyPlanLayer> layers = new ArrayList<>();

  /**
   * Legacy field from Stage 1–3 plans (flat list of nodes, all singles). Populated only when
   * deserialising an old-format JSON file; {@link DailyPlanManager} migrates these into SINGLE
   * layers on load and clears this field.
   */
  @Deprecated public List<DailyPlanNode> nodes;

  public DailyPlan() {}
}
