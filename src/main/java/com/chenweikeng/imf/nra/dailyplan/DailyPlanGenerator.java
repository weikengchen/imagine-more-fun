package com.chenweikeng.imf.nra.dailyplan;

import com.chenweikeng.imf.nra.config.ModConfig;
import com.chenweikeng.imf.nra.ride.AutograbHolder;
import com.chenweikeng.imf.nra.ride.RideCountManager;
import com.chenweikeng.imf.nra.ride.RideName;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Stage 1 generator: picks {@link #TARGET_NODE_COUNT} distinct rides at random from the eligible
 * pool and emits a flat linear plan, all singles with k={@link #DEFAULT_K}. Branching, OR/AND
 * nodes, and the constraint solver arrive in Stage 4.
 */
public final class DailyPlanGenerator {
  public static final int TARGET_NODE_COUNT = 5;
  public static final int DEFAULT_K = 2;

  private DailyPlanGenerator() {}

  public static DailyPlan generate(LocalDate today) {
    Random random = new Random();
    RideCountManager counts = RideCountManager.getInstance();

    List<RideName> candidates = new ArrayList<>();
    int maxGoal = ModConfig.currentSetting.maxGoal.getValue();
    boolean onlyAutograbbing = ModConfig.currentSetting.onlyAutograbbing;

    for (RideName ride : RideName.sortedByDisplayName()) {
      if (ModConfig.currentSetting.hiddenRides.contains(ride.toMatchString())) {
        continue;
      }
      if (onlyAutograbbing && !AutograbHolder.hasAutograb(ride)) {
        continue;
      }
      if (ride.getRideTime() >= 99999) {
        continue;
      }
      if (counts.getRideCount(ride) >= maxGoal) {
        continue;
      }
      candidates.add(ride);
    }

    Collections.shuffle(candidates, random);
    int pickCount = Math.min(TARGET_NODE_COUNT, candidates.size());

    DailyPlan plan = new DailyPlan();
    plan.date = today.toString();
    plan.generatedAtEpochMs = System.currentTimeMillis();
    plan.snapshotCounts = new HashMap<>();
    plan.nodes = new ArrayList<>();

    for (int i = 0; i < pickCount; i++) {
      RideName ride = candidates.get(i);
      plan.nodes.add(new DailyPlanNode(ride.toMatchString(), DEFAULT_K));
    }

    // Snapshot counts for every candidate, not just picked ones — lets Stage 2 credit via
    // match-name
    // lookup without branching on membership. Cheap (~60 ints).
    Map<String, Integer> snap = plan.snapshotCounts;
    for (RideName ride : RideName.values()) {
      snap.put(ride.toMatchString(), counts.getRideCount(ride));
    }

    return plan;
  }
}
