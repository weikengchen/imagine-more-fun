package com.chenweikeng.imf.pim.command;

import com.chenweikeng.imf.pim.PimClient;
import com.chenweikeng.imf.pim.pin.PinShortNameGenerator;
import com.chenweikeng.imf.pim.screen.PinDetailHandler;
import com.chenweikeng.imf.pim.screen.PinRarityHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class PimExportCommand {

  public static void register(
      com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
    dispatcher.register(
        ClientCommandManager.literal("pim:export")
            .requires(src -> PimClient.isImagineFunServer())
            .executes(
                context -> {
                  exportInventoryPins(context.getSource());
                  return 1;
                }));
  }

  private static void exportInventoryPins(FabricClientCommandSource source) {
    Minecraft.getInstance()
        .execute(
            () -> {
              try {
                Minecraft mc = Minecraft.getInstance();

                if (mc.player == null) {
                  source.sendFeedback(Component.literal("§6✨ §e[Pim] §cError: Player not found!"));
                  return;
                }

                String playerName = mc.player.getName().getString();

                PinShortNameGenerator shortNameGenerator = PinShortNameGenerator.getInstance();
                shortNameGenerator.generateShortNames();

                Map<String, List<String>> lookingForBySeries =
                    getLookingForPinsBySeries(shortNameGenerator);
                Map<String, List<String>> forSaleBySeries =
                    getForSalePinsBySeries(shortNameGenerator);

                StringBuilder exportText = new StringBuilder();
                exportText.append("**Player:** ").append(playerName);

                boolean hasLookingFor = !lookingForBySeries.isEmpty();
                boolean hasForSale = !forSaleBySeries.isEmpty();

                if (hasLookingFor) {
                  exportText.append("\n\n:lookingfor:\n");
                  for (Map.Entry<String, List<String>> entry : lookingForBySeries.entrySet()) {
                    String shortSeriesName = shortNameGenerator.getSeriesShortName(entry.getKey());
                    exportText
                        .append("- ")
                        .append(shortSeriesName)
                        .append(": ")
                        .append(String.join(" ", entry.getValue()))
                        .append("\n");
                  }
                }

                if (hasForSale) {
                  if (hasLookingFor) {
                    exportText.append("\n");
                  }
                  exportText.append(":forsale:\n");
                  for (Map.Entry<String, List<String>> entry : forSaleBySeries.entrySet()) {
                    String shortSeriesName = shortNameGenerator.getSeriesShortName(entry.getKey());
                    exportText
                        .append("- ")
                        .append(shortSeriesName)
                        .append(": ")
                        .append(String.join(" ", entry.getValue()))
                        .append("\n");
                  }
                }

                String finalExport = exportText.toString();

                try {
                  mc.keyboardHandler.setClipboard(finalExport);
                  source.sendFeedback(Component.literal("§6✨ §e[Pim] §a✓ Copied to clipboard!"));
                } catch (Exception e) {
                  source.sendFeedback(
                      Component.literal("§6✨ §e[Pim] §c⚠ Failed to copy to clipboard"));
                }
              } catch (Exception e) {
                source.sendFeedback(
                    Component.literal("§6✨ §e[Pim] §cError exporting pins: " + e.getMessage()));
              }
            });
  }

  private static Map<String, List<String>> getLookingForPinsBySeries(
      PinShortNameGenerator shortNameGenerator) {
    Map<String, List<String>> result = new TreeMap<>();

    PinRarityHandler rarityHandler = PinRarityHandler.getInstance();
    PinDetailHandler detailHandler = PinDetailHandler.getInstance();

    Set<String> playerMintPins = getPlayerDetailMintPins();

    Set<String> allSeriesNames = rarityHandler.getAllSeriesNames();

    for (String seriesName : allSeriesNames) {
      PinRarityHandler.PinSeriesEntry seriesEntry = rarityHandler.getSeriesEntry(seriesName);

      if (seriesEntry == null
          || seriesEntry.availability != PinRarityHandler.Availability.REQUIRED) {
        continue;
      }

      Map<String, PinDetailHandler.PinDetailEntry> detailMap =
          detailHandler.getSeriesDetails(seriesName);

      if (detailMap == null || detailMap.isEmpty()) {
        continue;
      }

      List<String> missingPins = new ArrayList<>();

      for (Map.Entry<String, PinDetailHandler.PinDetailEntry> pinEntry : detailMap.entrySet()) {
        String pinName = pinEntry.getKey();
        String key = seriesName + ":" + pinName;

        if (!playerMintPins.contains(key)) {
          String shortName = shortNameGenerator.getShortName(seriesName, pinName);
          missingPins.add(shortName);
        }
      }

      if (!missingPins.isEmpty()) {
        result.put(seriesName, missingPins);
      }
    }

    return result;
  }

  private static Map<String, List<String>> getForSalePinsBySeries(
      PinShortNameGenerator shortNameGenerator) {
    Map<String, List<String>> result = new TreeMap<>();

    Set<String> inventoryMintPins = com.chenweikeng.imf.pim.pin.Utils.getPlayerInventoryMintPins();

    for (String pinKey : inventoryMintPins) {
      String[] parts = pinKey.split(":");
      if (parts.length == 2) {
        String seriesName = parts[0];
        String pinName = parts[1];

        String shortName = shortNameGenerator.getShortName(seriesName, pinName);
        result.computeIfAbsent(seriesName, k -> new ArrayList<>()).add(shortName);
      }
    }

    return result;
  }

  private static Set<String> getPlayerDetailMintPins() {
    Set<String> mintPins = new java.util.HashSet<>();
    PinDetailHandler handler = PinDetailHandler.getInstance();

    Set<String> allSeries = handler.getAllSeriesNames();

    for (String seriesName : allSeries) {
      Map<String, PinDetailHandler.PinDetailEntry> seriesPins =
          handler.getSeriesDetails(seriesName);
      if (seriesPins != null) {
        for (Map.Entry<String, PinDetailHandler.PinDetailEntry> pinEntry : seriesPins.entrySet()) {
          String pinName = pinEntry.getKey();
          PinDetailHandler.PinDetailEntry detail = pinEntry.getValue();

          if (detail.condition == PinDetailHandler.PinCondition.MINT) {
            mintPins.add(seriesName + ":" + pinName);
          }
        }
      }
    }

    return mintPins;
  }
}
