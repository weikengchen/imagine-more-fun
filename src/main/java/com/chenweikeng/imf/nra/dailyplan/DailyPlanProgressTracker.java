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
    if (plan == null || plan.layers == null || plan.layers.isEmpty()) {
      return;
    }

    RideCountManager counts = RideCountManager.getInstance();
    boolean anyChanged = false;

    for (int layerIdx = 0; layerIdx < plan.layers.size(); layerIdx++) {
      DailyPlanLayer layer = plan.layers.get(layerIdx);
      if (layer.completed) {
        continue;
      }

      for (int nodeIdx = 0; nodeIdx < layer.nodes.size(); nodeIdx++) {
        DailyPlanNode node = layer.nodes.get(nodeIdx);
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
          DailyPlanCelebration.nodeCompleted(client, ride, nodeIdx, layer.nodes.size());
        }
      }

      if (layer.recomputeCompleted()) {
        DailyPlanCelebration.layerCompleted(client, layerIdx + 1, layer.type);
      }
    }

    boolean extended = DailyPlanGenerator.ensureTailCapacity(plan);
    if (anyChanged || extended) {
      DailyPlanStorage.save(plan);
    }
  }
}
