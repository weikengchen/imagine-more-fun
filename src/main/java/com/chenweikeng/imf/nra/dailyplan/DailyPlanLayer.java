package com.chenweikeng.imf.nra.dailyplan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * One step in the day's journey. A layer contains 1–3 nodes plus a {@link LayerType} that decides
 * when the layer counts as complete (all, any, or a majority of its nodes).
 */
public class DailyPlanLayer {
  public LayerType type;
  public List<DailyPlanNode> nodes;
  public boolean completed;

  /**
   * Per-ride baseline count captured the moment this layer became active. Null while the layer is
   * still gated (earlier layer not yet complete). Progress = currentCount - baseline, so rides done
   * before activation don't count.
   */
  public Map<String, Integer> baselineCounts;

  /**
   * True when the layer was injected by the plan generator from a parsed daily quest rather than
   * randomly chosen. Renders with a distinct gold treatment in the HUD. Quest layers also carry a
   * pre-set {@link #baselineCounts} so progress reflects the server-reported observed progress at
   * activation time.
   */
  public boolean fromDailyQuest;

  /**
   * Set to true when the chat reports {@code (!): Daily Objective Completed} for this layer's ride.
   * Used as the dedup oracle: a quest layer stays "pinned" (blocks re-adding the same ride) until
   * the server confirms completion, even after the local {@link #completed} flag flips. This
   * prevents the duplicate that occurs when local count reaches the target but the server hasn't
   * credited the quest yet — re-opening the Daily Objectives screen would otherwise re-capture the
   * still-pending quest and add a second layer.
   */
  public boolean serverCompleted;

  public DailyPlanLayer() {}

  public DailyPlanLayer(LayerType type, List<DailyPlanNode> nodes) {
    this.type = type;
    this.nodes = nodes;
    this.completed = false;
  }

  public static DailyPlanLayer single(DailyPlanNode node) {
    List<DailyPlanNode> list = new ArrayList<>(1);
    list.add(node);
    DailyPlanLayer layer = new DailyPlanLayer(LayerType.SINGLE, list);
    layer.completed = node.completed;
    return layer;
  }

  /**
   * Recomputes {@link #completed} from the current node states. Returns true if the layer's state
   * *flipped* from incomplete to complete on this call — useful for firing a one-shot layer
   * celebration.
   */
  public boolean recomputeCompleted() {
    boolean newState = evaluateCompleted();
    boolean flipped = newState && !completed;
    completed = newState;
    return flipped;
  }

  private boolean evaluateCompleted() {
    if (nodes == null || nodes.isEmpty() || type == null) {
      return false;
    }
    int doneCount = 0;
    for (DailyPlanNode node : nodes) {
      if (node.completed) {
        doneCount++;
      }
    }
    return switch (type) {
      case SINGLE -> nodes.get(0).completed;
      case OR -> doneCount >= 1;
      case AND -> doneCount == nodes.size();
      case TWO_OF_THREE -> doneCount >= 2;
    };
  }

  /** True once further rides of any ride in this layer can no longer change the completion. */
  public boolean isResolved() {
    return completed;
  }

  public List<DailyPlanNode> unmodifiableNodes() {
    return nodes == null ? List.of() : Collections.unmodifiableList(nodes);
  }

  public enum LayerType {
    SINGLE,
    OR,
    AND,
    TWO_OF_THREE;

    public String badge() {
      return switch (this) {
        case SINGLE -> "";
        case OR -> "OR";
        case AND -> "AND";
        case TWO_OF_THREE -> "2/3";
      };
    }
  }
}
