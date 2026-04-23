package com.chenweikeng.imf.nra.dailyplan;

import com.chenweikeng.imf.nra.config.ModConfig;
import com.chenweikeng.imf.nra.dailyplan.DailyPlanLayer.LayerType;
import com.chenweikeng.imf.nra.ride.AutograbHolder;
import com.chenweikeng.imf.nra.ride.RideCountManager;
import com.chenweikeng.imf.nra.ride.RideName;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Stage 4 generator: emits a list of {@link DailyPlanLayer}s with a mix of SINGLE/OR/AND/2-of-3
 * types, honouring a "no repeat within 2 layers" constraint. The plan is infinite — {@link
 * DailyPlanProgressTracker} calls {@link #appendLayers} to top up the tail as the player
 * progresses, so a session never runs out of next steps.
 */
public final class DailyPlanGenerator {
  public static final int INITIAL_LAYER_COUNT = 5;
  public static final int DEFAULT_K = 2;
  public static final int MIN_UNFINISHED_TAIL = 3;

  private static final int NO_REPEAT_WINDOW = 2;

  private DailyPlanGenerator() {}

  public static DailyPlan generate(LocalDate today) {
    RideCountManager counts = RideCountManager.getInstance();

    DailyPlan plan = new DailyPlan();
    plan.date = today.toString();
    plan.generatedAtEpochMs = System.currentTimeMillis();
    plan.snapshotCounts = new java.util.HashMap<>();
    for (RideName ride : RideName.values()) {
      plan.snapshotCounts.put(ride.toMatchString(), counts.getRideCount(ride));
    }
    plan.layers = new ArrayList<>();

    List<RideName> eligible = buildEligibleRides();
    Random random = new Random();
    appendLayers(plan, eligible, random, INITIAL_LAYER_COUNT);

    return plan;
  }

  public static List<RideName> buildEligibleRides() {
    int maxGoal = ModConfig.currentSetting.maxGoal.getValue();
    boolean onlyAutograbbing = ModConfig.currentSetting.onlyAutograbbing;
    RideCountManager counts = RideCountManager.getInstance();

    List<RideName> eligible = new ArrayList<>();
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
      eligible.add(ride);
    }
    return eligible;
  }

  /**
   * Appends up to {@code count} new layers. Skips any the generator cannot construct (e.g. out of
   * eligible rides).
   */
  public static void appendLayers(
      DailyPlan plan, List<RideName> eligible, Random random, int count) {
    if (plan.layers == null) {
      plan.layers = new ArrayList<>();
    }
    for (int i = 0; i < count; i++) {
      DailyPlanLayer layer = generateLayer(plan.layers, eligible, random);
      if (layer == null) {
        break;
      }
      plan.layers.add(layer);
    }
  }

  private static DailyPlanLayer generateLayer(
      List<DailyPlanLayer> existing, List<RideName> eligible, Random random) {
    if (eligible.isEmpty()) {
      return null;
    }

    Set<String> recent = ridesInLastLayers(existing, NO_REPEAT_WINDOW);
    List<RideName> candidates = new ArrayList<>();
    for (RideName ride : eligible) {
      if (!recent.contains(ride.toMatchString())) {
        candidates.add(ride);
      }
    }
    if (candidates.isEmpty()) {
      candidates = new ArrayList<>(eligible);
    }

    LayerType type = pickLayerType(existing.size(), random);
    int wantNodes = nodeCountFor(type, random);

    if (candidates.size() < wantNodes) {
      type = LayerType.SINGLE;
      wantNodes = 1;
      if (candidates.isEmpty()) {
        return null;
      }
    }

    Collections.shuffle(candidates, random);
    List<DailyPlanNode> nodes = new ArrayList<>(wantNodes);
    for (int i = 0; i < wantNodes; i++) {
      nodes.add(new DailyPlanNode(candidates.get(i).toMatchString(), DEFAULT_K));
    }

    return new DailyPlanLayer(type, nodes);
  }

  private static Set<String> ridesInLastLayers(List<DailyPlanLayer> existing, int windowSize) {
    Set<String> out = new HashSet<>();
    int start = Math.max(0, existing.size() - windowSize);
    for (int i = start; i < existing.size(); i++) {
      for (DailyPlanNode node : existing.get(i).nodes) {
        out.add(node.ride);
      }
    }
    return out;
  }

  private static LayerType pickLayerType(int layerIndex, Random random) {
    if (layerIndex == 0) {
      return LayerType.SINGLE;
    }

    // Every 3rd layer after index 0 prefers an act-break (AND or 2/3).
    if (layerIndex % 3 == 0) {
      return random.nextInt(100) < 25 ? LayerType.TWO_OF_THREE : LayerType.AND;
    }

    int roll = random.nextInt(100);
    if (roll < 50) {
      return LayerType.SINGLE;
    }
    if (roll < 85) {
      return LayerType.OR;
    }
    if (roll < 97) {
      return LayerType.AND;
    }
    return LayerType.TWO_OF_THREE;
  }

  private static int nodeCountFor(LayerType type, Random random) {
    return switch (type) {
      case SINGLE -> 1;
      case OR -> random.nextBoolean() ? 2 : 3;
      case AND -> 2;
      case TWO_OF_THREE -> 3;
    };
  }
}
