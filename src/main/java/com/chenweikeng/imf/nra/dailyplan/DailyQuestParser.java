package com.chenweikeng.imf.nra.dailyplan;

import com.chenweikeng.imf.nra.ride.RideName;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls a {@link DailyQuest} out of a quest-info item's lore lines. Only handles the two
 * ride-related goal verbs the server uses today:
 *
 * <ul>
 *   <li>{@code Goal: Ride <ride name> <N> times} → {@code target = N}
 *   <li>{@code Goal: Watch <ride name>} → {@code target = 1}
 * </ul>
 *
 * <p>Lines often wrap mid-name, so the goal block is reconstructed by joining consecutive non-blank
 * lines until the parser hits "Objective Progress:" or "Reward:".
 */
public final class DailyQuestParser {

  private static final Pattern RIDE_PATTERN = Pattern.compile("^Goal: Ride (.+) (\\d+) times?$");
  private static final Pattern WATCH_PATTERN = Pattern.compile("^Goal: Watch (.+)$");
  private static final Pattern PROGRESS_PATTERN =
      Pattern.compile("^Objective Progress: (\\d+) / (\\d+)$");
  private static final Pattern REWARD_PATTERN = Pattern.compile("^Reward: (\\d+) Kingdom Coins?$");

  private DailyQuestParser() {}

  /** Returns the parsed quest if the lore matches one of the supported verbs and a known ride. */
  public static Optional<DailyQuest> parse(List<String> loreLines) {
    if (loreLines == null || loreLines.isEmpty()) {
      return Optional.empty();
    }

    StringBuilder goalBuf = new StringBuilder();
    int observedProgress = -1;
    int rewardCoins = 0;

    for (String raw : loreLines) {
      String line = raw == null ? "" : raw.trim();
      if (line.isEmpty()) {
        continue;
      }
      Matcher progress = PROGRESS_PATTERN.matcher(line);
      if (progress.matches()) {
        observedProgress = Integer.parseInt(progress.group(1));
        continue;
      }
      Matcher reward = REWARD_PATTERN.matcher(line);
      if (reward.matches()) {
        rewardCoins = Integer.parseInt(reward.group(1));
        continue;
      }
      // Goal lines (possibly wrapped) — accumulate until Progress / Reward terminates them.
      if (goalBuf.length() > 0) {
        goalBuf.append(' ');
      }
      goalBuf.append(line);
    }

    String goal = goalBuf.toString();
    if (goal.isEmpty()) {
      return Optional.empty();
    }

    String rideName;
    int target;
    Matcher ride = RIDE_PATTERN.matcher(goal);
    Matcher watch = WATCH_PATTERN.matcher(goal);
    if (ride.matches()) {
      rideName = ride.group(1).trim();
      target = Integer.parseInt(ride.group(2));
    } else if (watch.matches()) {
      rideName = watch.group(1).trim();
      target = 1;
    } else {
      return Optional.empty();
    }

    if (RideName.fromMatchString(rideName) == RideName.UNKNOWN) {
      return Optional.empty();
    }

    int progress = observedProgress < 0 ? 0 : Math.min(observedProgress, target);
    return Optional.of(new DailyQuest(rideName, target, progress, rewardCoins));
  }
}
