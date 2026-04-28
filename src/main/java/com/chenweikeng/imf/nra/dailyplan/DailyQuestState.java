package com.chenweikeng.imf.nra.dailyplan;

import com.chenweikeng.imf.ImfStorage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Singleton holding the most-recent {@link DailyQuestSnapshot} parsed from the in-game Daily
 * Objectives screen. Backed by {@code config/imaginemorefun/nra-daily-quests.json} so the snapshot
 * survives restarts. The plan generator queries {@link #nextEligibleForPlan} when extending the
 * tail to decide whether the next layer should be a quest layer or a randomly generated one.
 */
public final class DailyQuestState {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private static DailyQuestState instance;

  private DailyQuestSnapshot snapshot;
  private boolean loaded;

  private DailyQuestState() {}

  public static synchronized DailyQuestState getInstance() {
    if (instance == null) {
      instance = new DailyQuestState();
    }
    return instance;
  }

  public synchronized DailyQuestSnapshot getSnapshot() {
    if (!loaded) {
      snapshot = loadFromDisk();
      loaded = true;
    }
    return snapshot;
  }

  public synchronized void setSnapshot(DailyQuestSnapshot fresh) {
    this.snapshot = fresh;
    this.loaded = true;
    saveToDisk(fresh);
  }

  /**
   * Returns the next quest the plan should pin as a new layer, or empty if either there are no
   * pending quests, the snapshot is stale, or every pending quest is already pinned in an
   * incomplete quest layer.
   */
  public synchronized Optional<DailyQuest> nextEligibleForPlan(DailyPlan plan) {
    DailyQuestSnapshot snap = getSnapshot();
    if (!isFresh(snap)) {
      return Optional.empty();
    }
    Set<String> alreadyPinned = ridesPinnedAsActiveQuests(plan);
    for (DailyQuest q : snap.quests) {
      if (q == null || q.rideMatchName == null) {
        continue;
      }
      if (q.observedProgress >= q.target) {
        continue;
      }
      if (alreadyPinned.contains(q.rideMatchName)) {
        continue;
      }
      return Optional.of(q);
    }
    return Optional.empty();
  }

  /**
   * "Same local date as today" — coarse but matches the plan's own daily reset and avoids surfacing
   * yesterday's quests after the server's overnight refresh. The user opens the screen at least
   * once per day under normal play.
   */
  static boolean isFresh(DailyQuestSnapshot snap) {
    if (snap == null || snap.quests == null || snap.quests.isEmpty()) {
      return false;
    }
    if (snap.capturedDate == null) {
      return false;
    }
    return snap.capturedDate.equals(LocalDate.now().toString());
  }

  private static Set<String> ridesPinnedAsActiveQuests(DailyPlan plan) {
    Set<String> out = new HashSet<>();
    if (plan == null || plan.layers == null) {
      return out;
    }
    for (DailyPlanLayer layer : plan.layers) {
      if (!layer.fromDailyQuest || layer.completed) {
        continue;
      }
      List<DailyPlanNode> nodes = layer.nodes;
      if (nodes == null) {
        continue;
      }
      for (DailyPlanNode node : nodes) {
        if (node != null && node.ride != null) {
          out.add(node.ride);
        }
      }
    }
    return out;
  }

  private static DailyQuestSnapshot loadFromDisk() {
    Path path = ImfStorage.nraDailyQuests();
    File file = path.toFile();
    if (!file.exists()) {
      return null;
    }
    try (FileReader reader = new FileReader(file)) {
      return GSON.fromJson(reader, DailyQuestSnapshot.class);
    } catch (IOException e) {
      return null;
    }
  }

  private static void saveToDisk(DailyQuestSnapshot snap) {
    if (snap == null) {
      return;
    }
    Path path = ImfStorage.nraDailyQuests();
    try {
      Files.createDirectories(path.getParent());
      try (FileWriter writer = new FileWriter(path.toFile())) {
        GSON.toJson(snap, writer);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
