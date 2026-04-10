package com.chenweikeng.imf.pim.command;

import com.chenweikeng.imf.pim.PimClient;
import com.chenweikeng.imf.pim.pin.Algorithm;
import com.chenweikeng.imf.pim.pin.Algorithm.DPResult;
import com.chenweikeng.imf.pim.pin.Algorithm.PinSeriesCounts;
import com.chenweikeng.imf.pim.screen.PinBookHandler;
import com.chenweikeng.imf.pim.screen.PinDetailHandler;
import com.chenweikeng.imf.pim.screen.PinRarityHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class PimComputeCommand {

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

  private static final ConcurrentHashMap<String, Algorithm.DPStartPoint> cachedStartPoints =
      new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<String, DPResult> cachedResults =
      new ConcurrentHashMap<>();

  public static void register(
      com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
    dispatcher.register(
        ClientCommandManager.literal("pim:compute")
            .requires(src -> PimClient.isImagineFunServer())
            .executes(
                context -> {
                  // Start calculation on separate thread
                  startCalculationThread(context.getSource());
                  return 1;
                }));
  }

  private static void startCalculationThread(FabricClientCommandSource source) {
    calculationExecutor.submit(
        () -> {
          try {
            processPinSeriesCalculations(source);
          } catch (Exception e) {
            Minecraft.getInstance()
                .execute(
                    () -> {
                      source.sendFeedback(
                          Component.literal(
                              "§6✨ §e[Pim] §cError during calculation: " + e.getMessage()));
                    });
          }
        });
  }

  private static void processPinSeriesCalculations(FabricClientCommandSource source) {
    // Get all series names from PinRarityHandler
    java.util.Set<String> allSeriesNames = PinRarityHandler.getInstance().getAllSeriesNames();

    if (allSeriesNames.isEmpty()) {
      Minecraft.getInstance()
          .execute(
              () -> {
                source.sendFeedback(
                    Component.literal(
                        "§6✨ §e[Pim] §cNo pin series data available. Please open /pinrarity and /pinbook first."));
              });
      return;
    }

    double totalDraws = 0;
    double totalPrice = 0;

    // Process each series
    for (String seriesName : allSeriesNames) {
      if (!isSeriesValidForCalculation(seriesName)) {
        continue;
      }

      String finalSeriesName = seriesName;

      try {
        // Get pin counts for this series
        PinSeriesCounts counts = getPinSeriesCounts(seriesName);
        if (counts == null) {
          continue;
        }

        // Check cache for start point
        Algorithm.DPStartPoint cachedStartPoint = cachedStartPoints.get(seriesName);

        DPResult result;
        if (cachedStartPoint != null && cachedStartPoint.equals(counts.startPoint)) {
          result = cachedResults.get(seriesName);
        } else {
          result = Algorithm.runDynamicProgramming(seriesName, counts);
          cachedStartPoints.put(seriesName, counts.startPoint);
          cachedResults.put(seriesName, result);
        }

        if (result == null || result.isError()) {
          Minecraft.getInstance()
              .execute(
                  () -> {
                    source.sendFeedback(
                        Component.literal(
                            "§6✨ §e[Pim] §cError calculating "
                                + finalSeriesName
                                + ": "
                                + (result != null ? result.error.get() : "Unknown error")));
                  });
          continue;
        }

        double value = result.value.get();
        double boxes = Math.round(value / 2.0);
        totalDraws += value;

        // Calculate price if series has price data
        String priceStr = null;
        double estimatedPrice = 0;
        PinRarityHandler.PinSeriesEntry seriesEntry =
            PinRarityHandler.getInstance().getSeriesEntry(finalSeriesName);
        if (seriesEntry != null && seriesEntry.color != null) {
          estimatedPrice = boxes * seriesEntry.color.price;
          priceStr = formatPrice(estimatedPrice);
          totalPrice += estimatedPrice;
        }

        final double finalBoxes = boxes;
        final String finalPriceStr = priceStr;

        Minecraft.getInstance()
            .execute(
                () -> {
                  if (result.isSuccess()) {
                    if (finalPriceStr != null) {
                      source.sendFeedback(
                          Component.literal(
                              "§6✨ §e[Pim] §a"
                                  + finalSeriesName
                                  + ": §f"
                                  + String.format("%.0f", finalBoxes)
                                  + " boxes (≈"
                                  + finalPriceStr
                                  + ")"));
                    } else {
                      source.sendFeedback(
                          Component.literal(
                              "§6✨ §e[Pim] §a"
                                  + finalSeriesName
                                  + ": §f"
                                  + String.format("%.0f", finalBoxes)
                                  + " boxes"));
                    }
                  } else if (result.isError()) {
                    source.sendFeedback(
                        Component.literal(
                            "§6✨ §e[Pim] §cError calculating "
                                + finalSeriesName
                                + ": "
                                + result.error.get()));
                  }
                });

        // Small delay between series to prevent overwhelming the system
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }

      } catch (Exception e) {
        Minecraft.getInstance()
            .execute(
                () -> {
                  source.sendFeedback(
                      Component.literal(
                          "§6✨ §e[Pim] §cError processing "
                              + finalSeriesName
                              + ": "
                              + e.getMessage()));
                });
      }
    }

    final double finalTotalDraws = totalDraws;
    final double finalTotalBoxes = finalTotalDraws / 2.0;
    final double finalTotalPrice = totalPrice;
    final String totalBoxesStr = formatPrice(finalTotalBoxes);
    final String totalPriceStr = finalTotalPrice > 0 ? formatPrice(finalTotalPrice) : "N/A";

    Minecraft.getInstance()
        .execute(
            () -> {
              source.sendFeedback(
                  Component.literal(
                      "§6✨ §e[Pim] §6Total: §f"
                          + totalBoxesStr
                          + " boxes"
                          + (finalTotalPrice > 0 ? " (≈" + totalPriceStr + ")" : "")));
            });
  }

  private static boolean isSeriesValidForCalculation(String seriesName) {
    // Check if series has complete information (no blinking condition)
    Map<String, PinDetailHandler.PinDetailEntry> detailMap =
        PinDetailHandler.getInstance().getSeriesDetails(seriesName);

    if (detailMap == null || detailMap.isEmpty()) {
      return false;
    }

    // Get PinBook entry to check totalMints
    PinBookHandler.PinBookEntry bookEntry = PinBookHandler.getInstance().getBookEntry(seriesName);
    if (bookEntry == null) {
      return false;
    }

    // Check if detailMap size matches totalMints (no blinking condition from line 92-94)
    if (detailMap.size() != bookEntry.totalMints) {
      return false;
    }

    // Check if the pin series is complete
    if (bookEntry.totalMints == bookEntry.mintsCollected) {
      return false;
    }

    // Check if all entries have rarity not null
    for (PinDetailHandler.PinDetailEntry entry : detailMap.values()) {
      if (entry.rarity == null) {
        return false;
      }
    }

    // Check if series is REQUIRED (ignore OPTIONAL series)
    PinRarityHandler.PinSeriesEntry seriesEntry =
        PinRarityHandler.getInstance().getSeriesEntry(seriesName);
    if (seriesEntry == null || seriesEntry.availability != PinRarityHandler.Availability.REQUIRED) {
      return false;
    }

    return true;
  }

  private static PinSeriesCounts getPinSeriesCounts(String seriesName) {
    try {
      // Get series details
      Map<String, PinDetailHandler.PinDetailEntry> detailMap =
          PinDetailHandler.getInstance().getSeriesDetails(seriesName);
      if (detailMap == null || detailMap.isEmpty()) {
        return null;
      }

      // Get book entry for total counts
      PinBookHandler.PinBookEntry bookEntry = PinBookHandler.getInstance().getBookEntry(seriesName);
      if (bookEntry == null) {
        return null;
      }

      // Count pins by rarity
      int signature = 0;
      int deluxe = 0;
      int rare = 0;
      int uncommon = 0;
      int common = 0;

      int mintSignature = 0;
      int mintDeluxe = 0;
      int mintRare = 0;
      int mintUncommon = 0;
      int mintCommon = 0;

      for (Map.Entry<String, PinDetailHandler.PinDetailEntry> entry : detailMap.entrySet()) {
        PinDetailHandler.PinDetailEntry detailEntry = entry.getValue();
        if (detailEntry.rarity == null) {
          continue; // Skip entries without rarity
        }

        switch (detailEntry.rarity) {
          case SIGNATURE:
            signature++;
            if (detailEntry.condition == PinDetailHandler.PinCondition.MINT) {
              mintSignature++;
            }
            break;
          case DELUXE:
            deluxe++;
            if (detailEntry.condition == PinDetailHandler.PinCondition.MINT) {
              mintDeluxe++;
            }
            break;
          case RARE:
            rare++;
            if (detailEntry.condition == PinDetailHandler.PinCondition.MINT) {
              mintRare++;
            }
            break;
          case UNCOMMON:
            uncommon++;
            if (detailEntry.condition == PinDetailHandler.PinCondition.MINT) {
              mintUncommon++;
            }
            break;
          case COMMON:
            common++;
            if (detailEntry.condition == PinDetailHandler.PinCondition.MINT) {
              mintCommon++;
            }
            break;
        }
      }

      // Create goal and start point
      Algorithm.DPGoal goal = new Algorithm.DPGoal(signature, deluxe, rare, uncommon, common);
      Algorithm.DPStartPoint startPoint =
          new Algorithm.DPStartPoint(mintSignature, mintDeluxe, mintRare, mintUncommon, mintCommon);

      return new PinSeriesCounts(goal, startPoint);

    } catch (Exception e) {
      return null;
    }
  }

  private static String formatPrice(double value) {
    if (value >= 1_000_000) {
      return String.format("%.1fM", value / 1_000_000);
    } else if (value >= 1_000) {
      return String.format("%.1fK", value / 1_000);
    } else {
      return String.format("%.0f", value);
    }
  }
}
