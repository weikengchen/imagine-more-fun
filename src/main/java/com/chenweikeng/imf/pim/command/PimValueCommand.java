package com.chenweikeng.imf.pim.command;

import com.chenweikeng.imf.pim.PimClient;
import com.chenweikeng.imf.pim.pin.PinCalculationUtils;
import com.chenweikeng.imf.pim.pin.Rarity;
import com.chenweikeng.imf.pim.screen.PinRarityHandler;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class PimValueCommand {

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
        ClientCommandManager.literal("pim:value")
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
            processValueCalculations(source);
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

  private static void processValueCalculations(FabricClientCommandSource source) {
    Minecraft.getInstance()
        .execute(
            () -> {
              source.sendFeedback(
                  Component.literal(
                      "§6✨ §e[Pim] §6Player Specific Values for Required Pin Series"));
            });

    Map<String, Map<Rarity, Double>> allPrices = new TreeMap<>();

    for (String seriesName : PinRarityHandler.getInstance().getAllSeriesNames()) {
      PinRarityHandler.PinSeriesEntry seriesEntry =
          PinRarityHandler.getInstance().getSeriesEntry(seriesName);

      if (seriesEntry != null
          && seriesEntry.availability == PinRarityHandler.Availability.REQUIRED) {
        Map<Rarity, Double> prices =
            PinCalculationUtils.calculatePlayerSpecificValuesForSeries(seriesName);
        if (!prices.isEmpty()) {
          allPrices.put(seriesName, prices);
        }
      }
    }

    if (allPrices.isEmpty()) {
      Minecraft.getInstance()
          .execute(
              () -> {
                source.sendFeedback(
                    Component.literal(
                        "§6✨ §e[Pim] §cNo player specific values available. Please ensure you have pin series data."));
              });
      return;
    }

    for (Map.Entry<String, Map<Rarity, Double>> seriesEntry : allPrices.entrySet()) {
      String seriesName = seriesEntry.getKey();
      Map<Rarity, Double> prices = seriesEntry.getValue();

      MutableComponent seriesLine =
          Component.literal("§e").append(Component.literal(seriesName + ":"));

      boolean hasAnyPrice = false;
      for (Rarity rarity : Rarity.values()) {
        Double price = prices.get(rarity);
        if (price != null) {
          seriesLine
              .append(
                  Component.literal(
                      " §f" + rarity.name().toLowerCase() + "§7=§a" + formatPrice(price)))
              .append(Component.literal("§7,"));
          hasAnyPrice = true;
        }
      }

      if (hasAnyPrice) {
        String seriesText = seriesLine.getString();
        if (seriesText.endsWith(",")) {
          seriesLine = Component.literal(seriesText.substring(0, seriesText.length() - 1));
        }

        final MutableComponent finalSeriesLine = seriesLine;
        Minecraft.getInstance()
            .execute(
                () -> {
                  source.sendFeedback(finalSeriesLine);
                });
      }
    }

    final int priceCount = allPrices.size();
    Minecraft.getInstance()
        .execute(
            () -> {
              source.sendFeedback(
                  Component.literal(
                      "§6✨ §e[Pim] §6Showing " + priceCount + " Required pin series with values"));
            });
  }

  private static String formatPrice(double value) {
    if (value == Math.floor(value)) {
      return String.format("%.0f", value);
    }
    return String.format("%.2f", value);
  }
}
