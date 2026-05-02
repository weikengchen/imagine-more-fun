package com.chenweikeng.imf.nra.canoe;

import com.chenweikeng.imf.ImfClient;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Writes one CSV file per canoe session under {@code <gameDir>/canoe-logs/}.
 *
 * <p>Each row is a single event ({@code TICK}, {@code CLICK_L}, {@code CLICK_R}, {@code AB}). Time
 * is milliseconds since session start (column {@code t_ms}).
 *
 * <p>Lifecycle is owned by {@link CanoeHelperClient}. Call {@link #open}, write rows, then {@link
 * #close}.
 */
public final class CanoeLogger {

  /** Single event types emitted to the CSV. */
  public enum EventType {
    /** Per-tick game state snapshot. */
    TICK,
    /** Left mouse click (attack) detected while holding canoe paddle. */
    CLICK_L,
    /** Right mouse click (use) detected while holding canoe paddle. */
    CLICK_R,
    /** Action-bar component update intercepted from {@code Gui.setOverlayMessage}. */
    AB,
  }

  private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  private static final String HEADER =
      "type,t_ms,player_x,player_y,player_z,player_yaw,"
          + "vehicle_id,vehicle_x,vehicle_y,vehicle_z,vehicle_yaw,"
          + "speed,fill,total,raw_action_bar";

  private final Path file;
  private final long startNanos;
  private PrintWriter out;

  private CanoeLogger(Path file, long startNanos) {
    this.file = file;
    this.startNanos = startNanos;
  }

  /** Create a new session log file and write the header. Returns null if I/O fails. */
  public static CanoeLogger open() {
    try {
      Path dir = FabricLoader.getInstance().getGameDir().resolve("canoe-logs");
      Files.createDirectories(dir);
      String name = "canoe-" + LocalDateTime.now().format(FILE_TS) + ".csv";
      Path file = dir.resolve(name);
      CanoeLogger logger = new CanoeLogger(file, System.nanoTime());
      logger.out =
          new PrintWriter(
              Files.newBufferedWriter(
                  file,
                  StandardCharsets.UTF_8,
                  StandardOpenOption.CREATE,
                  StandardOpenOption.APPEND),
              true);
      logger.out.println(HEADER);
      ImfClient.LOGGER.info("[Canoe] logging session to {}", file);
      return logger;
    } catch (IOException e) {
      ImfClient.LOGGER.error("[Canoe] failed to open log file", e);
      return null;
    }
  }

  /** Stops writing and releases the file handle. Idempotent. */
  public synchronized void close() {
    if (out != null) {
      out.flush();
      out.close();
      out = null;
      ImfClient.LOGGER.info("[Canoe] log session closed: {}", file);
    }
  }

  /** Returns true if this logger is still writable. */
  public boolean isOpen() {
    return out != null;
  }

  /** Returns the path of the current log file (for diagnostics). */
  public Path getFile() {
    return file;
  }

  /** Append a row. Thread-safe. {@code raw} is CSV-escaped. */
  public synchronized void write(
      EventType type,
      double playerX,
      double playerY,
      double playerZ,
      float playerYaw,
      int vehicleId,
      Double vehicleX,
      Double vehicleY,
      Double vehicleZ,
      Float vehicleYaw,
      float speed,
      int fill,
      int total,
      String raw) {
    if (out == null) return;
    long tMs = (System.nanoTime() - startNanos) / 1_000_000L;
    StringBuilder sb = new StringBuilder(128);
    sb.append(type.name()).append(',');
    sb.append(tMs).append(',');
    sb.append(fmt(playerX)).append(',');
    sb.append(fmt(playerY)).append(',');
    sb.append(fmt(playerZ)).append(',');
    sb.append(fmt(playerYaw)).append(',');
    sb.append(vehicleId).append(',');
    sb.append(vehicleX == null ? "" : fmt(vehicleX)).append(',');
    sb.append(vehicleY == null ? "" : fmt(vehicleY)).append(',');
    sb.append(vehicleZ == null ? "" : fmt(vehicleZ)).append(',');
    sb.append(vehicleYaw == null ? "" : fmt(vehicleYaw)).append(',');
    sb.append(Float.isNaN(speed) ? "" : fmt(speed)).append(',');
    sb.append(fill < 0 ? "" : Integer.toString(fill)).append(',');
    sb.append(total < 0 ? "" : Integer.toString(total)).append(',');
    sb.append(csvEscape(raw));
    out.println(sb);
  }

  private static String fmt(double v) {
    // 4 dp is enough resolution for blocks (~6mm) and yaw (~0.0001°).
    return String.format(java.util.Locale.ROOT, "%.4f", v);
  }

  private static String csvEscape(String s) {
    if (s == null) return "";
    boolean needsQuote = false;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == ',' || c == '"' || c == '\n' || c == '\r') {
        needsQuote = true;
        break;
      }
    }
    if (!needsQuote) return s;
    StringBuilder sb = new StringBuilder(s.length() + 4);
    sb.append('"');
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '"') sb.append('"');
      sb.append(c);
    }
    sb.append('"');
    return sb.toString();
  }
}
