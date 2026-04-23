package com.chenweikeng.imf.nra.dailyplan;

import java.time.LocalDate;
import java.util.ArrayList;

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
        boolean migrated = migrateLegacyIfNeeded(cached);
        if (migrated) {
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
