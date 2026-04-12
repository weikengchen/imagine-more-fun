package com.chenweikeng.imf.nra.tracker;

import com.chenweikeng.imf.nra.NotRidingAlertClient;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.phys.Vec3;

/**
 * Tracks quest target locations using triangulation from boss bar distance readings. Maintains a
 * buffer of recently sent positions to correlate with server distance updates, compensating for
 * network latency.
 */
public final class QuestTriangulationTracker {

  private static final QuestTriangulationTracker INSTANCE = new QuestTriangulationTracker();

  // Pattern to extract distance from boss bar name: "Quest: ... (616.9) ..."
  private static final Pattern DISTANCE_PATTERN = Pattern.compile("\\((\\d+\\.?\\d*)\\)");

  // Position buffer - keeps recent positions sent to server
  private static final int POSITION_BUFFER_SIZE = 100; // ~5 seconds at 20 TPS
  private final Deque<TimestampedPosition> positionBuffer = new ArrayDeque<>();

  // Collected triangulation samples
  private static final int MAX_SAMPLES = 20;
  private final List<TriangulationSample> samples = new ArrayList<>();

  // Estimated RTT in milliseconds (can be updated from ping)
  private long estimatedRttMs = 100;

  // Minimum distance between samples to avoid redundant data
  private static final double MIN_SAMPLE_DISTANCE = 10.0;

  // UUID of the boss bar being tracked (to avoid mixing different quests)
  private UUID trackedBossBarId = null;

  // Current quest being tracked
  private String currentQuestPrefix = null;

  // Estimated target location
  private Vec3 estimatedTarget = null;
  private double estimationError = Double.MAX_VALUE;

  // Last known distance for detecting sudden jumps (step completion)
  private double lastKnownDistance = -1;
  private static final double DISTANCE_JUMP_THRESHOLD = 50.0; // If distance jumps by 50+ blocks
  // When player is close to target, use a smaller threshold for step detection
  private static final double CLOSE_TO_TARGET_RADIUS = 15.0; // Within 15 blocks of target
  private static final double CLOSE_DISTANCE_JUMP_THRESHOLD = 10.0; // Smaller jump when close

  // Sample timeout - discard samples older than this (milliseconds)
  private static final long SAMPLE_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes

  private QuestTriangulationTracker() {}

  public static QuestTriangulationTracker getInstance() {
    return INSTANCE;
  }

  /** Called when client sends a position packet to server. */
  public void recordSentPosition(Vec3 position) {
    long timestamp = System.currentTimeMillis();
    synchronized (positionBuffer) {
      positionBuffer.addLast(new TimestampedPosition(timestamp, position));
      // Trim old entries
      while (positionBuffer.size() > POSITION_BUFFER_SIZE) {
        positionBuffer.removeFirst();
      }
    }
  }

  /** Called when a boss bar is added or its name is updated. */
  public void onBossBarUpdate(UUID bossBarId, String bossBarName) {
    // Extract distance from name
    Matcher matcher = DISTANCE_PATTERN.matcher(bossBarName);
    if (!matcher.find()) {
      return;
    }

    double distance;
    try {
      distance = Double.parseDouble(matcher.group(1));
    } catch (NumberFormatException e) {
      return;
    }

    // Extract quest prefix (everything before the distance)
    String questPrefix = bossBarName.substring(0, matcher.start()).trim();

    // Check if this is a different boss bar or new quest
    if (trackedBossBarId == null) {
      // First boss bar with distance - start tracking
      trackedBossBarId = bossBarId;
      currentQuestPrefix = questPrefix;
      samples.clear();
      estimatedTarget = null;
      estimationError = Double.MAX_VALUE;
      lastKnownDistance = -1;
      NotRidingAlertClient.LOGGER.info(
          "[QuestTriangulation] Started tracking quest: {} (UUID: {})", questPrefix, bossBarId);
    } else if (!bossBarId.equals(trackedBossBarId)) {
      // Different boss bar - switch to this one
      trackedBossBarId = bossBarId;
      currentQuestPrefix = questPrefix;
      samples.clear();
      estimatedTarget = null;
      estimationError = Double.MAX_VALUE;
      lastKnownDistance = -1;
      NotRidingAlertClient.LOGGER.info(
          "[QuestTriangulation] Switched to new quest: {} (UUID: {})", questPrefix, bossBarId);
    } else if (!questPrefix.equals(currentQuestPrefix)) {
      // Same boss bar but quest changed (next step)
      currentQuestPrefix = questPrefix;
      samples.clear();
      estimatedTarget = null;
      estimationError = Double.MAX_VALUE;
      lastKnownDistance = -1;
      NotRidingAlertClient.LOGGER.info("[QuestTriangulation] Quest step changed: {}", questPrefix);
    } else if (lastKnownDistance > 0) {
      // Check for distance jump that indicates step completion
      double distanceChange = Math.abs(distance - lastKnownDistance);

      // Determine threshold based on whether player was close to the target
      double threshold = DISTANCE_JUMP_THRESHOLD;
      boolean wasCloseToTarget = false;

      if (estimatedTarget != null && hasConfidentEstimate()) {
        // Check if the last known distance was small (player was close to target)
        if (lastKnownDistance < CLOSE_TO_TARGET_RADIUS) {
          threshold = CLOSE_DISTANCE_JUMP_THRESHOLD;
          wasCloseToTarget = true;
        }
      }

      if (distanceChange > threshold) {
        // Same boss bar, same text, but distance jumped
        // This indicates a quest step completion where NPC moved to new location
        samples.clear();
        estimatedTarget = null;
        estimationError = Double.MAX_VALUE;
        NotRidingAlertClient.LOGGER.info(
            "[QuestTriangulation] Distance jumped from {} to {} (threshold: {}, closeToTarget: {})"
                + " - clearing samples for new target",
            String.format("%.1f", lastKnownDistance),
            String.format("%.1f", distance),
            String.format("%.1f", threshold),
            wasCloseToTarget);
      }
    }

    // Find the position that corresponds to this distance
    // Look back RTT/2 in the buffer (server received position RTT/2 ago)
    long targetTimestamp = System.currentTimeMillis() - (estimatedRttMs / 2);
    Vec3 serverPosition = findPositionAtTime(targetTimestamp);

    if (serverPosition == null) {
      NotRidingAlertClient.LOGGER.debug(
          "[QuestTriangulation] No position found for timestamp {}", targetTimestamp);
      return;
    }

    // Check if this sample is far enough from existing samples
    if (!isSampleDistinct(serverPosition)) {
      return;
    }

    // Add sample
    long now = System.currentTimeMillis();
    TriangulationSample sample = new TriangulationSample(serverPosition, distance, now);
    samples.add(sample);

    NotRidingAlertClient.LOGGER.info(
        "[QuestTriangulation] Sample {}: pos=({}, {}, {}), dist={}",
        samples.size(),
        String.format("%.1f", serverPosition.x),
        String.format("%.1f", serverPosition.y),
        String.format("%.1f", serverPosition.z),
        String.format("%.1f", distance));

    // Remove old samples
    pruneOldSamples();

    // Trim to max samples if needed
    while (samples.size() > MAX_SAMPLES) {
      samples.remove(0);
    }

    // Update last known distance
    lastKnownDistance = distance;

    // Try to estimate target if we have enough samples
    if (samples.size() >= 3) {
      estimateTarget();
    }
  }

  /** Called when a boss bar is removed. */
  public void onBossBarRemoved(UUID bossBarId) {
    if (bossBarId.equals(trackedBossBarId)) {
      NotRidingAlertClient.LOGGER.info(
          "[QuestTriangulation] Quest boss bar removed, clearing tracking data");
      trackedBossBarId = null;
      currentQuestPrefix = null;
      samples.clear();
      estimatedTarget = null;
      estimationError = Double.MAX_VALUE;
      lastKnownDistance = -1;
    }
  }

  /** Remove samples older than the timeout. */
  private void pruneOldSamples() {
    long cutoff = System.currentTimeMillis() - SAMPLE_TIMEOUT_MS;
    samples.removeIf(s -> s.timestamp < cutoff);
  }

  /** Find position in buffer closest to the given timestamp. */
  private Vec3 findPositionAtTime(long targetTimestamp) {
    synchronized (positionBuffer) {
      TimestampedPosition closest = null;
      long closestDiff = Long.MAX_VALUE;

      for (TimestampedPosition tp : positionBuffer) {
        long diff = Math.abs(tp.timestamp - targetTimestamp);
        if (diff < closestDiff) {
          closestDiff = diff;
          closest = tp;
        }
      }

      return closest != null ? closest.position : null;
    }
  }

  /** Check if a new sample position is distinct enough from existing samples. */
  private boolean isSampleDistinct(Vec3 position) {
    for (TriangulationSample sample : samples) {
      if (sample.position.distanceTo(position) < MIN_SAMPLE_DISTANCE) {
        return false;
      }
    }
    return true;
  }

  /** Estimate target location using least squares on collected samples. */
  private void estimateTarget() {
    if (samples.size() < 3) {
      return;
    }

    // Use the last N samples for estimation
    int n = Math.min(samples.size(), 10);
    List<TriangulationSample> useSamples = samples.subList(samples.size() - n, samples.size());

    // Check if Y values vary enough for 3D estimation
    double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
    for (TriangulationSample s : useSamples) {
      minY = Math.min(minY, s.position.y);
      maxY = Math.max(maxY, s.position.y);
    }
    boolean use3D = (maxY - minY) > 2.0; // Need at least 2 blocks of Y variation

    if (use3D) {
      estimate3D(useSamples);
    } else {
      estimate2D(useSamples);
    }
  }

  /** 3D triangulation when Y values vary. */
  private void estimate3D(List<TriangulationSample> useSamples) {
    int n = useSamples.size();
    TriangulationSample s1 = useSamples.get(0);

    double[][] A = new double[n - 1][3];
    double[] b = new double[n - 1];

    for (int i = 1; i < n; i++) {
      TriangulationSample si = useSamples.get(i);
      A[i - 1][0] = 2 * (si.position.x - s1.position.x);
      A[i - 1][1] = 2 * (si.position.y - s1.position.y);
      A[i - 1][2] = 2 * (si.position.z - s1.position.z);

      b[i - 1] =
          s1.distance * s1.distance
              - si.distance * si.distance
              + si.position.x * si.position.x
              - s1.position.x * s1.position.x
              + si.position.y * si.position.y
              - s1.position.y * s1.position.y
              + si.position.z * si.position.z
              - s1.position.z * s1.position.z;
    }

    // Compute A^T * A (3x3)
    double[][] ATA = new double[3][3];
    for (int i = 0; i < n - 1; i++) {
      for (int j = 0; j < 3; j++) {
        for (int k = 0; k < 3; k++) {
          ATA[j][k] += A[i][j] * A[i][k];
        }
      }
    }

    // Compute A^T * b (3x1)
    double[] ATb = new double[3];
    for (int i = 0; i < n - 1; i++) {
      for (int j = 0; j < 3; j++) {
        ATb[j] += A[i][j] * b[i];
      }
    }

    // Solve 3x3 system using Cramer's rule
    double det = det3(ATA);
    if (Math.abs(det) < 1e-10) {
      NotRidingAlertClient.LOGGER.warn(
          "[QuestTriangulation] Cannot estimate 3D: samples may be collinear");
      // Fall back to 2D
      estimate2D(useSamples);
      return;
    }

    double tx = det3(replaceCol(ATA, 0, ATb)) / det;
    double ty = det3(replaceCol(ATA, 1, ATb)) / det;
    double tz = det3(replaceCol(ATA, 2, ATb)) / det;

    estimatedTarget = new Vec3(tx, ty, tz);
    calculateError(useSamples);

    NotRidingAlertClient.LOGGER.info(
        "[QuestTriangulation] 3D estimate: ({}, {}, {}), avg error: {}",
        String.format("%.1f", tx),
        String.format("%.1f", ty),
        String.format("%.1f", tz),
        String.format("%.1f", estimationError));
  }

  /** 2D triangulation when Y values don't vary - solve for X,Z only, estimate Y from distance. */
  private void estimate2D(List<TriangulationSample> useSamples) {
    int n = useSamples.size();
    TriangulationSample s1 = useSamples.get(0);

    // 2D: only X and Z
    double[][] A = new double[n - 1][2];
    double[] b = new double[n - 1];

    for (int i = 1; i < n; i++) {
      TriangulationSample si = useSamples.get(i);
      A[i - 1][0] = 2 * (si.position.x - s1.position.x);
      A[i - 1][1] = 2 * (si.position.z - s1.position.z);

      b[i - 1] =
          s1.distance * s1.distance
              - si.distance * si.distance
              + si.position.x * si.position.x
              - s1.position.x * s1.position.x
              + si.position.z * si.position.z
              - s1.position.z * s1.position.z;
    }

    // Compute A^T * A (2x2)
    double[][] ATA = new double[2][2];
    for (int i = 0; i < n - 1; i++) {
      for (int j = 0; j < 2; j++) {
        for (int k = 0; k < 2; k++) {
          ATA[j][k] += A[i][j] * A[i][k];
        }
      }
    }

    // Compute A^T * b (2x1)
    double[] ATb = new double[2];
    for (int i = 0; i < n - 1; i++) {
      for (int j = 0; j < 2; j++) {
        ATb[j] += A[i][j] * b[i];
      }
    }

    // Solve 2x2 system
    double det = ATA[0][0] * ATA[1][1] - ATA[0][1] * ATA[1][0];
    if (Math.abs(det) < 1e-10) {
      NotRidingAlertClient.LOGGER.warn(
          "[QuestTriangulation] Cannot estimate 2D: samples may be collinear");
      return;
    }

    double tx = (ATb[0] * ATA[1][1] - ATb[1] * ATA[0][1]) / det;
    double tz = (ATA[0][0] * ATb[1] - ATA[1][0] * ATb[0]) / det;

    // Estimate Y: use the average Y of samples, then adjust based on distance residual
    double avgY = useSamples.stream().mapToDouble(s -> s.position.y).average().orElse(64.0);

    // Calculate horizontal distance to estimated point from sample 1
    double dx = tx - s1.position.x;
    double dz = tz - s1.position.z;
    double horizontalDistSq = dx * dx + dz * dz;

    // If measured distance is larger than horizontal distance, there's a Y component
    double verticalDistSq = s1.distance * s1.distance - horizontalDistSq;
    double ty;
    if (verticalDistSq > 0) {
      double verticalDist = Math.sqrt(verticalDistSq);
      // Could be above or below - for now assume at similar level or above
      ty = avgY + verticalDist * 0.5; // Bias slightly upward
    } else {
      ty = avgY;
    }

    estimatedTarget = new Vec3(tx, ty, tz);
    calculateError(useSamples);

    NotRidingAlertClient.LOGGER.info(
        "[QuestTriangulation] 2D estimate: ({}, {}, {}), avg error: {} (Y uncertain)",
        String.format("%.1f", tx),
        String.format("%.1f", ty),
        String.format("%.1f", tz),
        String.format("%.1f", estimationError));
  }

  private void calculateError(List<TriangulationSample> useSamples) {
    double totalError = 0;
    for (TriangulationSample sample : useSamples) {
      double calculatedDist = sample.position.distanceTo(estimatedTarget);
      totalError += Math.abs(calculatedDist - sample.distance);
    }
    estimationError = totalError / useSamples.size();
  }

  private double det3(double[][] m) {
    return m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1])
        - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0])
        + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);
  }

  private double[][] replaceCol(double[][] m, int col, double[] vec) {
    double[][] r = new double[3][3];
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        r[i][j] = (j == col) ? vec[i] : m[i][j];
      }
    }
    return r;
  }

  /** Update RTT estimate (can be called with ping value). */
  public void updateRtt(long rttMs) {
    this.estimatedRttMs = Math.max(20, rttMs);
  }

  /** Get the estimated target location, or null if not enough data. */
  public Vec3 getEstimatedTarget() {
    return estimatedTarget;
  }

  /** Get the estimation error (average distance error in blocks). */
  public double getEstimationError() {
    return estimationError;
  }

  /** Get the number of samples collected. */
  public int getSampleCount() {
    return samples.size();
  }

  /** Get the current quest being tracked. */
  public String getCurrentQuest() {
    return currentQuestPrefix;
  }

  /** Get the UUID of the tracked boss bar. */
  public UUID getTrackedBossBarId() {
    return trackedBossBarId;
  }

  /** Check if we have a confident estimate (low error with enough samples). */
  public boolean hasConfidentEstimate() {
    return estimatedTarget != null && samples.size() >= 5 && estimationError < 5.0;
  }

  /** Reset all tracking data. */
  public void reset() {
    synchronized (positionBuffer) {
      positionBuffer.clear();
    }
    samples.clear();
    trackedBossBarId = null;
    currentQuestPrefix = null;
    estimatedTarget = null;
    estimationError = Double.MAX_VALUE;
    lastKnownDistance = -1;
  }

  private record TimestampedPosition(long timestamp, Vec3 position) {}

  private record TriangulationSample(Vec3 position, double distance, long timestamp) {}
}
