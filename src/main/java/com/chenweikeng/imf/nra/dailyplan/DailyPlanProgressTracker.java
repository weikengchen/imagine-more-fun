package com.chenweikeng.imf.nra.dailyplan;

import com.chenweikeng.imf.nra.ride.RideCountManager;
import com.chenweikeng.imf.nra.ride.RideName;
import net.minecraft.client.Minecraft;

public final class DailyPlanProgressTracker {
  private static DailyPlanProgressTracker instance;

  private DailyPlanProgressTracker() {}

  public static synchronized DailyPlanProgressTracker getInstance() {
    if (instance == null) {
      instance = new DailyPlanProgressTracker();
    }
    return instance;
  }

  public void tick(Minecraft client) {
    if (client == null || client.player == null || client.level == null) {
      return;
    }
    DailyPlan plan = DailyPlanManager.getInstance().getOrCreateToday();
    if (plan == null || plan.nodes == null || plan.nodes.isEmpty()) {
      return;
    }

    RideCountManager counts = RideCountManager.getInstance();
    boolean anyChanged = false;

    for (int i = 0; i < plan.nodes.size(); i++) {
      DailyPlanNode node = plan.nodes.get(i);
      if (node.completed) {
        continue;
      }

      RideName ride = RideName.fromMatchString(node.ride);
      if (ride == RideName.UNKNOWN) {
        continue;
      }

      Integer snap = plan.snapshotCounts == null ? null : plan.snapshotCounts.get(node.ride);
      int baseline = snap == null ? 0 : snap;
      int delta = counts.getRideCount(ride) - baseline;

      if (delta >= node.k) {
        node.completed = true;
        anyChanged = true;
        DailyPlanCelebration.nodeCompleted(client, ride, i, plan.nodes.size());
      }
    }

    if (anyChanged) {
      DailyPlanStorage.save(plan);
      if (allNodesComplete(plan)) {
        DailyPlanCelebration.planCompleted(client);
      }
    }
  }

  private boolean allNodesComplete(DailyPlan plan) {
    for (DailyPlanNode node : plan.nodes) {
      if (!node.completed) {
        return false;
      }
    }
    return true;
  }
}
