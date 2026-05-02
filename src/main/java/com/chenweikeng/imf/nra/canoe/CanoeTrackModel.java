package com.chenweikeng.imf.nra.canoe;

import com.chenweikeng.imf.ImfClient;
import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * The reference path of "Davy Crockett's Explorer Canoes", baked from ride 110700 (a clean
 * representative trip). At runtime, given a {@code (x, z)} on the boat, we find the closest point
 * on the path and read its precomputed cumulative arc length to give a 0..100 % progress reading.
 *
 * <p>The path is resampled at uniform 2-block spacing ({@code N ≈ 580} points); brute-force
 * nearest-point search is fast enough that no spatial index is needed.
 *
 * <p>The boat's track is a loop — start ({@code 568, 502}) and end ({@code 543, 487}) are close
 * together. Naive Euclidean distance from start would alias mid-ride positions with start/end. The
 * arc-length projection avoids that because the closest waypoint is always evaluated locally.
 */
public final class CanoeTrackModel {

  private static final String RESOURCE_PATH = "/data/imaginemorefun/canoe/track.json";

  private static volatile CanoeTrackModel instance;

  private final float[] xs;
  private final float[] zs;
  private final float[] cumDist;
  private final float totalLength;

  private CanoeTrackModel(float[] xs, float[] zs, float[] cumDist, float totalLength) {
    this.xs = xs;
    this.zs = zs;
    this.cumDist = cumDist;
    this.totalLength = totalLength;
  }

  public static CanoeTrackModel get() {
    CanoeTrackModel local = instance;
    if (local == null) {
      synchronized (CanoeTrackModel.class) {
        local = instance;
        if (local == null) {
          local = load();
          instance = local;
        }
      }
    }
    return local;
  }

  private static CanoeTrackModel load() {
    try (InputStream in = CanoeTrackModel.class.getResourceAsStream(RESOURCE_PATH)) {
      if (in == null) {
        ImfClient.LOGGER.error("[Canoe] track resource missing at {}", RESOURCE_PATH);
        return new CanoeTrackModel(new float[0], new float[0], new float[0], 0f);
      }
      TrackJson parsed =
          new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), TrackJson.class);
      int n = parsed.points.length;
      float[] xs = new float[n];
      float[] zs = new float[n];
      float[] dd = new float[n];
      for (int i = 0; i < n; i++) {
        xs[i] = parsed.points[i].x;
        zs[i] = parsed.points[i].z;
        dd[i] = parsed.points[i].d;
      }
      ImfClient.LOGGER.info(
          "[Canoe] track loaded: {} points, total {} blocks", n, parsed.total_length);
      return new CanoeTrackModel(xs, zs, dd, parsed.total_length);
    } catch (Exception e) {
      ImfClient.LOGGER.error("[Canoe] failed to load track", e);
      return new CanoeTrackModel(new float[0], new float[0], new float[0], 0f);
    }
  }

  public boolean isLoaded() {
    return xs.length > 0;
  }

  public float totalLength() {
    return totalLength;
  }

  /**
   * Project {@code (x, z)} onto the reference path and return the cumulative arc length at the
   * closest waypoint, in blocks. Returns -1 if the track isn't loaded.
   */
  public float arcLengthAt(double x, double z) {
    if (xs.length == 0) return -1f;
    int best = 0;
    float bestSq = Float.POSITIVE_INFINITY;
    for (int i = 0; i < xs.length; i++) {
      float dx = (float) (x - xs[i]);
      float dz = (float) (z - zs[i]);
      float sq = dx * dx + dz * dz;
      if (sq < bestSq) {
        bestSq = sq;
        best = i;
      }
    }
    return cumDist[best];
  }

  /** Distance to the closest waypoint, in blocks. Useful to detect "off-track". */
  public float distanceToTrack(double x, double z) {
    if (xs.length == 0) return Float.POSITIVE_INFINITY;
    float bestSq = Float.POSITIVE_INFINITY;
    for (int i = 0; i < xs.length; i++) {
      float dx = (float) (x - xs[i]);
      float dz = (float) (z - zs[i]);
      float sq = dx * dx + dz * dz;
      if (sq < bestSq) bestSq = sq;
    }
    return (float) Math.sqrt(bestSq);
  }

  /** 0..100 progress along the reference track, or -1 if not loaded / off-track. */
  public int progressPercent(double x, double z) {
    if (xs.length == 0 || totalLength <= 0) return -1;
    float arc = arcLengthAt(x, z);
    if (arc < 0) return -1;
    int p = Math.round(arc / totalLength * 100f);
    if (p < 0) p = 0;
    if (p > 100) p = 100;
    return p;
  }

  /* ---------- JSON shape ---------- */

  private static final class TrackJson {
    String source;
    float spacing_blocks;
    float total_length;
    Point[] points;
  }

  private static final class Point {
    float x;
    float z;
    float d;
  }
}
