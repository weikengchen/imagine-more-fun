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

    int activeIdx = -1;
    for (int i = 0; i < plan.layers.size(); i++) {
      if (!plan.layers.get(i).completed) {
        activeIdx = i;
        break;
      }
    }

    if (activeIdx >= 0) {
      DailyPlanLayer layer = plan.layers.get(activeIdx);
      if (layer.baselineCounts == null) {
        layer.baselineCounts = new java.util.HashMap<>();
        for (DailyPlanNode node : layer.nodes) {
          RideName r = RideName.fromMatchString(node.ride);
          layer.baselineCounts.put(node.ride, counts.getRideCount(r));
        }
        anyChanged = true;
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
        int baseline = layer.baselineCounts.getOrDefault(node.ride, 0);
        int delta = counts.getRideCount(ride) - baseline;

        if (delta >= node.k) {
          node.completed = true;
          anyChanged = true;
          DailyPlanCelebration.nodeCompleted(client, ride, nodeIdx, layer.nodes.size());
        }
      }

      if (layer.recomputeCompleted()) {
        DailyPlanCelebration.layerCompleted(client, activeIdx + 1, layer.type);
      }
    }

    boolean extended = DailyPlanGenerator.ensureTailCapacity(plan);
    if (anyChanged || extended) {
      DailyPlanStorage.save(plan);
    }
  }
}
