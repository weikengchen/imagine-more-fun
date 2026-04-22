package com.chenweikeng.imf.nra.dailyplan;

import java.time.LocalDate;

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

  /** Returns today's plan, generating and persisting one if none exists or the stored one is */
  /* stale. */
  public synchronized DailyPlan getOrCreateToday() {
    LocalDate today = LocalDate.now();
    if (!loaded) {
      cached = DailyPlanStorage.load();
      loaded = true;
    }
    if (cached == null || !today.toString().equals(cached.date)) {
      cached = DailyPlanGenerator.generate(today);
      DailyPlanStorage.save(cached);
    }
    return cached;
  }
}
