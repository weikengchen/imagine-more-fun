package com.chenweikeng.imf.nra.config.profile;

import com.chenweikeng.imf.nra.NotRidingAlertClient;
import com.chenweikeng.imf.nra.config.ConfigSetting;
import com.chenweikeng.imf.nra.config.ModConfig;
import com.chenweikeng.imf.nra.report.DailyRideSnapshot;
import com.chenweikeng.imf.nra.ride.RideCountManager;
import com.chenweikeng.imf.nra.ride.RideName;
import com.chenweikeng.imf.nra.session.SessionTracker;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class DataBundleExporter {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Boolean FILE_DIALOGS_AVAILABLE = checkFileDialogsAvailable();

  /** Returns true if the platform supports native file dialogs. */
  public static boolean isFileDialogAvailable() {
    return FILE_DIALOGS_AVAILABLE;
  }

  private static boolean checkFileDialogsAvailable() {
    String os = System.getProperty("os.name", "").toLowerCase();
    if (os.contains("mac") || os.contains("darwin") || os.contains("win")) {
      return true;
    }
    // Linux: check if zenity is installed
    try {
      return new ProcessBuilder("which", "zenity").redirectErrorStream(true).start().waitFor() == 0;
    } catch (Exception e) {
      return false;
    }
  }

  private DataBundleExporter() {}

  public static class ExportBundle {
    public int version = 1;
    public long exportedAt;
    public ConfigSetting settings;
    public Map<String, DailyRideSnapshot.SnapshotEntry> rideSnapshots;
    public Map<String, Integer> rideCounts;
  }

  public static ExportBundle createBundle() {
    ExportBundle bundle = new ExportBundle();
    bundle.exportedAt = System.currentTimeMillis();
    bundle.settings = ModConfig.currentSetting.copy();

    bundle.rideSnapshots = DailyRideSnapshot.getInstance().getAllSnapshots();

    RideCountManager countManager = RideCountManager.getInstance();
    Map<RideName, Integer> allCounts = countManager.getAllRideCounts();
    bundle.rideCounts = new HashMap<>();
    for (Map.Entry<RideName, Integer> entry : allCounts.entrySet()) {
      if (entry.getKey() != RideName.UNKNOWN) {
        bundle.rideCounts.put(entry.getKey().toMatchString(), entry.getValue());
      }
    }

    return bundle;
  }

  public static void exportToFile(Path filePath) throws Exception {
    ExportBundle bundle = createBundle();
    try (FileWriter writer = new FileWriter(filePath.toFile())) {
      GSON.toJson(bundle, writer);
    }
  }

  public static ImportResult importFromFile(Path filePath) {
    try {
      ExportBundle bundle;
      try (FileReader reader = new FileReader(filePath.toFile())) {
        bundle = GSON.fromJson(reader, ExportBundle.class);
      }

      if (bundle == null) {
        return new ImportResult(false, "Empty or invalid file");
      }
      if (bundle.version != 1) {
        return new ImportResult(false, "Unsupported format version: " + bundle.version);
      }
      if (bundle.settings == null) {
        return new ImportResult(false, "No settings found in file");
      }

      // Apply settings
      ModConfig.currentSetting = bundle.settings.copy();
      ModConfig.save();

      // Merge ride snapshots
      int snapshotCount = 0;
      if (bundle.rideSnapshots != null) {
        snapshotCount = bundle.rideSnapshots.size();
        DailyRideSnapshot.getInstance().mergeSnapshots(bundle.rideSnapshots);
      }

      // Merge cumulative ride counts
      int rideCountUpdates = 0;
      if (bundle.rideCounts != null) {
        RideCountManager countManager = RideCountManager.getInstance();
        for (Map.Entry<String, Integer> entry : bundle.rideCounts.entrySet()) {
          RideName ride = RideName.fromMatchString(entry.getKey());
          if (ride != RideName.UNKNOWN) {
            int before = countManager.getRideCount(ride);
            countManager.importRideCount(ride, entry.getValue());
            if (countManager.getRideCount(ride) > before) {
              rideCountUpdates++;
            }
          }
        }
        countManager.forceSave();
      }

      // Realign today's session counters with the new cumulative totals. After cumulative
      // counts jump on import, the daily report's "delta from previous day" can show many more
      // rides than DailySessionData.ridesCompleted (which only tracks rides registered live in
      // this client). Compute the implied today-count from cumulative - prevDaySnapshot and
      // bump the session counters via SessionTracker.
      boolean sessionRealigned = realignSessionFromImport();

      return new ImportResult(
          true,
          "Imported settings, "
              + snapshotCount
              + " day(s) of history, "
              + rideCountUpdates
              + " ride count update(s)"
              + (sessionRealigned ? ", today's counters realigned" : ""));

    } catch (Exception e) {
      NotRidingAlertClient.LOGGER.error("Failed to import data bundle", e);
      return new ImportResult(false, "Import failed: " + e.getMessage());
    }
  }

  /**
   * After ride counts and snapshots have been merged, recompute today's implied ride count from
   * (currentCumulative - mostRecentPriorSnapshot) and bump SessionTracker's ride counter with max
   * semantics. Returns true if the counter was actually changed.
   *
   * <p>Only the ride count is bumped — never the ride time. Imported rides happened in another
   * client at another time, so attributing their duration to today's online session would push the
   * "ride time per online hour" HUD metric well past its 60 m/hr ceiling.
   */
  private static boolean realignSessionFromImport() {
    DailyRideSnapshot snapshots = DailyRideSnapshot.getInstance();
    String todayDate = LocalDate.now().toString();
    String prevDate = snapshots.getMostRecentDateBefore(todayDate);
    if (prevDate == null) {
      return false;
    }
    DailyRideSnapshot.SnapshotEntry prevSnap = snapshots.getSnapshot(prevDate);
    if (prevSnap == null || prevSnap.rideCounts == null) {
      return false;
    }
    Map<String, Integer> prevCounts = prevSnap.rideCounts;
    Map<RideName, Integer> liveCounts = RideCountManager.getInstance().getAllRideCounts();
    int impliedRides = 0;
    for (Map.Entry<RideName, Integer> e : liveCounts.entrySet()) {
      RideName ride = e.getKey();
      if (ride == RideName.UNKNOWN) continue;
      int prev = prevCounts.getOrDefault(ride.toMatchString(), 0);
      int increase = e.getValue() - prev;
      if (increase > 0) {
        impliedRides += increase;
      }
    }
    if (impliedRides <= 0) {
      return false;
    }
    return SessionTracker.getInstance().realignTodayFromImport(impliedRides);
  }

  /** Opens a native save dialog on a background thread. Returns null path on cancel. */
  public static CompletableFuture<Path> pickExportFile() {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            String defaultName = "nra-export-" + LocalDate.now() + ".json";
            String os = System.getProperty("os.name", "").toLowerCase();
            String output;

            if (os.contains("mac") || os.contains("darwin")) {
              output =
                  runProcess(
                      "osascript",
                      "-e",
                      "POSIX path of (choose file name with prompt \"Export NRA Data\""
                          + " default name \""
                          + defaultName
                          + "\")");
            } else if (os.contains("win")) {
              output =
                  runProcess(
                      "powershell",
                      "-NoProfile",
                      "-Command",
                      "Add-Type -AssemblyName System.Windows.Forms;"
                          + "$d = New-Object System.Windows.Forms.SaveFileDialog;"
                          + "$d.Filter = 'JSON files (*.json)|*.json';"
                          + "$d.FileName = '"
                          + defaultName
                          + "';"
                          + "if ($d.ShowDialog() -eq 'OK') { $d.FileName }");
            } else {
              output =
                  runProcess(
                      "zenity",
                      "--file-selection",
                      "--save",
                      "--confirm-overwrite",
                      "--title=Export NRA Data",
                      "--filename=" + defaultName,
                      "--file-filter=JSON files | *.json");
            }

            if (output == null || output.isEmpty()) return null;
            Path path = Path.of(output);
            if (!output.endsWith(".json")) {
              path = Path.of(output + ".json");
            }
            return path;
          } catch (Exception e) {
            NotRidingAlertClient.LOGGER.warn("Failed to open save dialog", e);
            return null;
          }
        });
  }

  /** Opens a native open dialog on a background thread. Returns null path on cancel. */
  public static CompletableFuture<Path> pickImportFile() {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            String os = System.getProperty("os.name", "").toLowerCase();
            String output;

            if (os.contains("mac") || os.contains("darwin")) {
              output =
                  runProcess(
                      "osascript",
                      "-e",
                      "POSIX path of (choose file of type {\"public.json\"}"
                          + " with prompt \"Import NRA Data\")");
            } else if (os.contains("win")) {
              output =
                  runProcess(
                      "powershell",
                      "-NoProfile",
                      "-Command",
                      "Add-Type -AssemblyName System.Windows.Forms;"
                          + "$d = New-Object System.Windows.Forms.OpenFileDialog;"
                          + "$d.Filter = 'JSON files (*.json)|*.json';"
                          + "$d.Title = 'Import NRA Data';"
                          + "if ($d.ShowDialog() -eq 'OK') { $d.FileName }");
            } else {
              output =
                  runProcess(
                      "zenity",
                      "--file-selection",
                      "--title=Import NRA Data",
                      "--file-filter=JSON files | *.json");
            }

            if (output == null || output.isEmpty()) return null;
            return Path.of(output);
          } catch (Exception e) {
            NotRidingAlertClient.LOGGER.warn("Failed to open file dialog", e);
            return null;
          }
        });
  }

  private static String runProcess(String... command) throws Exception {
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes()).trim();
    int exitCode = process.waitFor();
    if (exitCode != 0 || output.isEmpty()) return null;
    return output;
  }

  public record ImportResult(boolean success, String message) {}
}
