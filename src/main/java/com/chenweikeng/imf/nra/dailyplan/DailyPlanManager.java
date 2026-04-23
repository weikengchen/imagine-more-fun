package com.chenweikeng.imf.nra.dailyplan;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

public final class DailyPlanManager {
  private static DailyPlanManager instance;

  private DailyPlan cached;
  private boolean loaded;

  private DailyPlanManager() {}

  public static synchronized DailyPlanManager getInstance() {
    if (instance == null) {
      instance = new DailyPlanManager();
    }
    return instance;
  }

  /** Returns today's plan, generating (or migrating) one if none exists or the stored one is */
  /* stale. */
  public synchronized DailyPlan getOrCreateToday() {
    LocalDate today = LocalDate.now();
    if (!loaded) {
      cached = DailyPlanStorage.load();
      loaded = true;
      if (cached != null) {
        boolean migratedNodes = migrateLegacyIfNeeded(cached);
        boolean migratedBaselines = migrateBaselinesIfNeeded(cached);
        if (migratedNodes || migratedBaselines) {
          DailyPlanStorage.save(cached);
        }
      }
    }
    boolean regenerated = false;
    if (cached == null
        || !today.toString().equals(cached.date)
        || cached.layers == null
        || cached.layers.isEmpty()) {
      cached = DailyPlanGenerator.generate(today);
      regenerated = true;
    }
    boolean extended = DailyPlanGenerator.ensureTailCapacity(cached);
    if (regenerated || extended) {
      DailyPlanStorage.save(cached);
    }
    return cached;
  }

  /** Re-reads from disk — useful when external state (e.g. file deleted) changes. */
  public synchronized void invalidateCache() {
    cached = null;
    loaded = false;
  }

  /**
   * Wraps each legacy {@code nodes} entry in a SINGLE layer, preserving {@code completed} state.
   * Returns true if migration happened and the caller should persist.
   */
  /**
   * Pre-gated-era plans have no per-layer baselines. Seed the active layer and all earlier ones
   * from {@code plan.snapshotCounts} so their visible progress doesn't reset; leave later layers
   * null so gating picks up from the next activation.
   */
  private static boolean migrateBaselinesIfNeeded(DailyPlan plan) {
    if (plan.layers == null || plan.layers.isEmpty()) {
      return false;
    }
    boolean anyBaselineSet = false;
    for (DailyPlanLayer layer : plan.layers) {
      if (layer.baselineCounts != null) {
        anyBaselineSet = true;
        break;
      }
    }
    if (anyBaselineSet) {
      return false;
    }
    int activeIdx = -1;
    for (int i = 0; i < plan.layers.size(); i++) {
      if (!plan.layers.get(i).completed) {
        activeIdx = i;
        break;
      }
    }
    int endIdx = activeIdx == -1 ? plan.layers.size() - 1 : activeIdx;
    java.util.Map<String, Integer> snap =
        plan.snapshotCounts == null ? new HashMap<>() : plan.snapshotCounts;
    for (int i = 0; i <= endIdx; i++) {
      plan.layers.get(i).baselineCounts = new HashMap<>(snap);
    }
    return true;
  }

  private static boolean migrateLegacyIfNeeded(DailyPlan plan) {
    if (plan.layers != null && !plan.layers.isEmpty()) {
      plan.nodes = null;
      return false;
    }
    if (plan.nodes == null || plan.nodes.isEmpty()) {
      return false;
    }
    plan.layers = new ArrayList<>(plan.nodes.size());
    for (DailyPlanNode node : plan.nodes) {
      plan.layers.add(DailyPlanLayer.single(node));
    }
    plan.nodes = null;
    return true;
  }
}
