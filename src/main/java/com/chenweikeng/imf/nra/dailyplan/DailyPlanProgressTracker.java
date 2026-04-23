package com.chenweikeng.imf.nra.dailyplan;

import com.chenweikeng.imf.nra.ride.RideCountManager;
import com.chenweikeng.imf.nra.ride.RideName;
import java.util.List;
import java.util.Random;
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

    if (anyChanged) {
      ensureTailCapacity(plan);
      DailyPlanStorage.save(plan);
    }
  }

  /** If the plan has fewer than {@code MIN_UNFINISHED_TAIL} open layers, top it up. */
  private static void ensureTailCapacity(DailyPlan plan) {
    int unfinished = 0;
    for (DailyPlanLayer layer : plan.layers) {
      if (!layer.completed) {
        unfinished++;
      }
    }
    int needed = DailyPlanGenerator.MIN_UNFINISHED_TAIL - unfinished;
    if (needed <= 0) {
      return;
    }
    List<RideName> eligible = DailyPlanGenerator.buildEligibleRides();
    Random random = new Random();
    DailyPlanGenerator.appendLayers(plan, eligible, random, needed);
  }
}
