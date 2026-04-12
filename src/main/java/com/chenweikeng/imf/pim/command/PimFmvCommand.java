package com.chenweikeng.imf.pim.command;

import com.chenweikeng.imf.pim.PimClient;
import com.chenweikeng.imf.pim.pin.PinCalculationUtils;
import com.chenweikeng.imf.pim.pin.PinShortNameGenerator;
import com.chenweikeng.imf.pim.pin.Rarity;
import com.chenweikeng.imf.pim.screen.PinRarityHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class PimFmvCommand {
  private static final Map<String, PinCalculationUtils.FMVResult> fmvCache =
      new java.util.TreeMap<>();
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private static final File dataFile = new File("config/pim_fmv.json");

  private static final ExecutorService calculationExecutor =
      Executors.newSingleThreadExecutor(
          new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);
            private final ClassLoader contextClassLoader =
                Thread.currentThread().getContextClassLoader();

            @Override
            public Thread newThread(Runnable r) {
              Thread thread = new Thread(r, "Pim-Calculation-" + counter.incrementAndGet());
              thread.setDaemon(true);
              thread.setPriority(Thread.MIN_PRIORITY);
              thread.setContextClassLoader(contextClassLoader);
              return thread;
            }
          });

  public static void register(
      com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
    dispatcher.register(
        ClientCommandManager.literal("pim:fmv")
            .requires(src -> PimClient.isImagineFunServer())
            .executes(
                context -> {
                  startCalculationThread(context.getSource());
                  return 1;
                }));
  }

  private static void startCalculationThread(FabricClientCommandSource source) {
    calculationExecutor.submit(
        () -> {
          try {
            processFmvCalculations(source);
          } catch (Exception e) {
            Minecraft.getInstance()
                .execute(
                    () -> {
                      source.sendFeedback(
                          Component.literal(
                              "§6✨ §e[IMF] §cError during calculation: " + e.getMessage()));
                    });
          }
        });
  }

  private static void processFmvCalculations(FabricClientCommandSource source) {
    loadCache();

    Minecraft.getInstance()
        .execute(
            () -> {
              source.sendFeedback(
                  Component.literal("§6✨ §e[IMF] §6FMV Values for Required Pin Series"));
            });

    Map<String, PinCalculationUtils.FMVResult> allResults = new java.util.TreeMap<>();
    boolean cacheMiss = false;
    StringBuilder clipboardText = new StringBuilder();

    for (String seriesName : PinRarityHandler.getInstance().getAllSeriesNames()) {
      PinRarityHandler.PinSeriesEntry seriesEntry =
          PinRarityHandler.getInstance().getSeriesEntry(seriesName);

      if (seriesEntry != null
          && seriesEntry.availability == PinRarityHandler.Availability.REQUIRED) {
        PinCalculationUtils.FMVResult fmvResult;

        if (fmvCache.containsKey(seriesName)) {
          fmvResult = fmvCache.get(seriesName);

          if (fmvResult.floorValues.isEmpty() || fmvResult.ceilValues.isEmpty()) {
            fmvResult = PinCalculationUtils.calculateFMVValuesForSeries(seriesName);
            fmvCache.put(seriesName, fmvResult);
            cacheMiss = true;
          }
        } else {
          fmvResult = PinCalculationUtils.calculateFMVValuesForSeries(seriesName);
          fmvCache.put(seriesName, fmvResult);
          cacheMiss = true;
        }

        if (!fmvResult.floorValues.isEmpty() || !fmvResult.ceilValues.isEmpty()) {
          allResults.put(seriesName, fmvResult);
        }
      }
    }

    if (allResults.isEmpty()) {
      Minecraft.getInstance()
          .execute(
              () -> {
                source.sendFeedback(
                    Component.literal(
                        "§6✨ §e[IMF] §cNo FMV values available. Please ensure you have pin series data."));
              });
      return;
    }

    PinShortNameGenerator shortNameGenerator = PinShortNameGenerator.getInstance();
    shortNameGenerator.generateShortNames();

    for (Map.Entry<String, PinCalculationUtils.FMVResult> seriesEntry : allResults.entrySet()) {
      String seriesName = seriesEntry.getKey();
      PinCalculationUtils.FMVResult fmvResult = seriesEntry.getValue();

      String shortSeriesName = shortNameGenerator.getSeriesShortName(seriesName);

      MutableComponent seriesLine =
          Component.literal("§e").append(Component.literal(seriesName + ":"));

      StringBuilder seriesClipboardText = new StringBuilder(shortSeriesName + ":");

      boolean hasAnyPrice = false;
      for (Rarity rarity : Rarity.values()) {
        Double floorPrice = fmvResult.floorValues.get(rarity);
        Double ceilPrice = fmvResult.ceilValues.get(rarity);
        if (floorPrice != null || ceilPrice != null) {
          int floorInt = floorPrice != null ? (int) Math.round(floorPrice) : 0;
          int ceilInt = ceilPrice != null ? (int) Math.round(ceilPrice) : 0;

          String priceText;
          if (floorInt == ceilInt) {
            priceText = String.valueOf(floorInt);
          } else {
            priceText = floorInt + "-" + ceilInt;
          }

          String rarityAbbr;
          switch (rarity) {
            case SIGNATURE:
              rarityAbbr = "S";
              break;
            case DELUXE:
              rarityAbbr = "D";
              break;
            case RARE:
              rarityAbbr = "R";
              break;
            case UNCOMMON:
              rarityAbbr = "U";
              break;
            case COMMON:
              rarityAbbr = "C";
              break;
            default:
              rarityAbbr = rarity.name().toLowerCase();
              break;
          }

          seriesLine
              .append(Component.literal(" §f" + rarity.name().toLowerCase() + "§7=§a" + priceText))
              .append(Component.literal("§7,"));

          seriesClipboardText
              .append(" ")
              .append(rarityAbbr)
              .append("=")
              .append(priceText)
              .append(",");
          hasAnyPrice = true;
        }
      }

      if (hasAnyPrice) {
        String seriesText = seriesLine.getString();
        if (seriesText.endsWith(",")) {
          seriesLine = Component.literal(seriesText.substring(0, seriesText.length() - 1));
        }

        if (seriesClipboardText.length() > 0
            && seriesClipboardText.charAt(seriesClipboardText.length() - 1) == ',') {
          seriesClipboardText.setLength(seriesClipboardText.length() - 1);
        }

        clipboardText.append(seriesClipboardText).append("\n");

        final MutableComponent finalSeriesLine = seriesLine;
        Minecraft.getInstance()
            .execute(
                () -> {
                  source.sendFeedback(finalSeriesLine);
                });
      }
    }

    if (cacheMiss) {
      saveCache();
    }

    final int resultCount = allResults.size();
    final String finalClipboardText = clipboardText.toString();

    Component button =
        Component.literal("[Click to Copy]")
            .setStyle(
                Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(finalClipboardText)));

    Minecraft.getInstance()
        .execute(
            () -> {
              MutableComponent message =
                  Component.literal(
                      "§6✨ §e[IMF] §6Showing "
                          + resultCount
                          + " Required pin series with FMV values ");

              if (!finalClipboardText.isEmpty()) {
                message = message.append(button);
              }

              source.sendFeedback(message);
            });
  }

  private static void loadCache() {
    if (!dataFile.exists()) {
      return;
    }

    try (FileReader reader = new FileReader(dataFile)) {
      Type mapType = new TypeToken<Map<String, PinCalculationUtils.FMVResult>>() {}.getType();
      Map<String, PinCalculationUtils.FMVResult> loadedMap = gson.fromJson(reader, mapType);
      if (loadedMap != null) {
        fmvCache.putAll(loadedMap);
      }
    } catch (IOException e) {
      PimClient.LOGGER.error("[Pim] Failed to load FMV cache", e);
    }
  }

  private static void saveCache() {
    if (dataFile.getParentFile() != null && !dataFile.getParentFile().exists()) {
      dataFile.getParentFile().mkdirs();
    }

    try (FileWriter writer = new FileWriter(dataFile)) {
      gson.toJson(fmvCache, writer);
    } catch (IOException e) {
      PimClient.LOGGER.error("[Pim] Failed to save FMV cache", e);
    }
  }

  public static void resetCache() {
    fmvCache.clear();

    if (dataFile.exists()) {
      if (dataFile.delete()) {
        PimClient.LOGGER.info("[Pim] FMV cache file deleted successfully");
      } else {
        PimClient.LOGGER.warn("[Pim] Failed to delete FMV cache file");
      }
    }
  }
}
