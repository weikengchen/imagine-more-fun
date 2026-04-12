package com.chenweikeng.imf.pim.pinpack;

import com.chenweikeng.imf.pim.pin.Utils;
import com.chenweikeng.imf.pim.screen.PinDetailHandler;
import com.chenweikeng.imf.pim.screen.PinRarityHandler;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Analyzes pin packs to determine their display color based on contents.
 *
 * <p>Colors: - MAGENTA: No star (useless pack) - GREEN: Has star + contains at least one NEEDED pin
 * - YELLOW: Has star + only duplicate mints
 */
public final class PinPackColorAnalyzer {

  public static final int COLOR_MAGENTA = 0x80FF00FF;
  public static final int COLOR_GREEN = 0xFF00FF00;
  public static final int COLOR_YELLOW = 0xFFFFE000;
  public static final int COLOR_SILVER = 0xFFC0C0C0;

  public enum PackStatus {
    NOT_A_PACK,
    NO_STAR,
    UNOPENED,
    HAS_NEEDED,
    ONLY_DUPLICATES
  }

  private PinPackColorAnalyzer() {}

  /**
   * Analyzes a pin pack and returns its status.
   *
   * @param stack The ItemStack to analyze
   * @return PackStatus indicating the pack's value
   */
  public static PackStatus analyze(ItemStack stack) {
    if (stack.isEmpty()) {
      return PackStatus.NOT_A_PACK;
    }

    String name = stack.getHoverName().getString();
    if (!name.contains("Pin Pack")) {
      return PackStatus.NOT_A_PACK;
    }

    // Check if opened first - unopened packs get silver regardless of star
    CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
    boolean isOpened = name.contains("(Opened)");

    if (customData != null) {
      CompoundTag nbt = customData.copyTag();
      String nbtString = nbt.toString();
      isOpened = isOpened || nbtString.contains("IS_PACK_OPENED:1b");

      if (!isOpened) {
        // Unopened pack - show silver background
        return PackStatus.UNOPENED;
      }

      // Opened pack - check for star indicator
      boolean hasStar = name.contains("\u2b50");

      if (!hasStar) {
        return PackStatus.NO_STAR;
      }

      // Opened star pack - check the pins inside
      return analyzeOpenedPack(nbt);
    }

    // No custom data - check name for opened status
    if (!isOpened) {
      return PackStatus.UNOPENED;
    }

    // Opened but no custom data - treat as no star
    return PackStatus.NO_STAR;
  }

  private static PackStatus analyzeOpenedPack(CompoundTag nbt) {
    // Get the player's current mint pins
    Set<String> playerMintPins = Utils.getPlayerInventoryMintPins();

    // Parse PACK_ITEM_1 and PACK_ITEM_2
    String packItem1Json = nbt.getString("PACK_ITEM_1").orElse(null);
    String packItem2Json = nbt.getString("PACK_ITEM_2").orElse(null);

    boolean hasNeeded = false;

    if (packItem1Json != null && !packItem1Json.isEmpty()) {
      if (isPinNeeded(packItem1Json, playerMintPins)) {
        hasNeeded = true;
      }
    }

    if (!hasNeeded && packItem2Json != null && !packItem2Json.isEmpty()) {
      if (isPinNeeded(packItem2Json, playerMintPins)) {
        hasNeeded = true;
      }
    }

    return hasNeeded ? PackStatus.HAS_NEEDED : PackStatus.ONLY_DUPLICATES;
  }

  private static boolean isPinNeeded(String pinJson, Set<String> playerMintPins) {
    try {
      JsonObject json = JsonParser.parseString(pinJson).getAsJsonObject();

      // Get pin name
      String pinName = json.has("name") ? json.get("name").getAsString() : null;
      if (pinName == null) {
        return false;
      }
      // Strip color codes from pin name
      pinName = stripColorCodes(pinName);

      // Get series info
      String seriesName = null;
      if (json.has("series") && json.get("series").isJsonObject()) {
        JsonObject series = json.getAsJsonObject("series");
        if (series.has("name")) {
          seriesName = series.get("name").getAsString();
        }
      }
      if (seriesName == null) {
        return false;
      }

      // Check if this is a REQUIRED series
      PinRarityHandler.PinSeriesEntry seriesEntry =
          PinRarityHandler.getInstance().getSeriesEntry(seriesName);
      if (seriesEntry == null
          || seriesEntry.availability != PinRarityHandler.Availability.REQUIRED) {
        return false;
      }

      // Check condition - only mint condition pins matter
      String conditionId = null;
      if (json.has("condition") && json.get("condition").isJsonObject()) {
        JsonObject condition = json.getAsJsonObject("condition");
        if (condition.has("id")) {
          conditionId = condition.get("id").getAsString();
        }
      }

      if (!"BRAND_NEW_MINT_CONDITION".equals(conditionId)) {
        return false;
      }

      // Check if player already has this mint pin
      String key = seriesName + ":" + pinName;
      if (playerMintPins.contains(key)) {
        return false; // Already have it - duplicate
      }

      // Check existing collection
      PinDetailHandler.PinDetailEntry existingEntry =
          PinDetailHandler.getInstance().findDetailEntry(seriesName, pinName);
      if (existingEntry != null && existingEntry.condition == PinDetailHandler.PinCondition.MINT) {
        return false; // Already collected as mint
      }

      // This pin is needed!
      return true;

    } catch (Exception e) {
      return false;
    }
  }

  private static String stripColorCodes(String text) {
    if (text == null) {
      return null;
    }
    // Strip Minecraft color codes (§X) and legacy Bukkit codes (&X)
    return text.replaceAll("[§&][0-9a-fk-or]", "");
  }

  /**
   * Gets the fill color for a pin pack based on its status.
   *
   * @param stack The ItemStack to analyze
   * @return The color to use, or null if not a pack
   */
  public static Integer getPackColor(ItemStack stack) {
    PackStatus status = analyze(stack);
    switch (status) {
      case NO_STAR:
        return COLOR_MAGENTA;
      case UNOPENED:
        return COLOR_SILVER;
      case HAS_NEEDED:
        return COLOR_GREEN;
      case ONLY_DUPLICATES:
        return COLOR_YELLOW;
      default:
        return null;
    }
  }

  /**
   * Gets the fill color for a pin pack in the shop based on series completion.
   *
   * @param stack The ItemStack to analyze (shop items have PIN_PACK NBT tag)
   * @return COLOR_MAGENTA if series is complete, null otherwise
   */
  public static Integer getShopPackColor(ItemStack stack) {
    if (stack.isEmpty()) {
      return null;
    }

    // Check for PIN_PACK NBT tag to identify shop pack items
    CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
    if (customData == null) {
      return null;
    }

    CompoundTag nbt = customData.copyTag();
    String pinPackId = nbt.getString("PIN_PACK").orElse(null);
    if (pinPackId == null || pinPackId.isEmpty()) {
      return null;
    }

    // Get the display name and strip "#N" suffix to get series name
    // e.g., "Country Series #1" -> "Country Series"
    String name = stack.getHoverName().getString();
    String seriesName = name.replaceAll(" #\\d+$", "");

    if (isSeriesComplete(seriesName)) {
      return COLOR_MAGENTA;
    }

    return null;
  }

  /**
   * Checks if a pin series is complete (all pins collected as MINT).
   *
   * @param seriesName The series name to check
   * @return true if all pins in the series are MINT condition
   */
  public static boolean isSeriesComplete(String seriesName) {
    // Check if this is a REQUIRED series
    PinRarityHandler.PinSeriesEntry seriesEntry =
        PinRarityHandler.getInstance().getSeriesEntry(seriesName);
    if (seriesEntry == null || seriesEntry.availability != PinRarityHandler.Availability.REQUIRED) {
      // Optional series or unknown - not considered "complete" for shop purposes
      return false;
    }

    // Get the detail map for this series
    var detailMap = PinDetailHandler.getInstance().getSeriesDetails(seriesName);
    if (detailMap == null || detailMap.isEmpty()) {
      // No data - can't determine, assume not complete
      return false;
    }

    // Check if all pins are MINT
    for (var entry : detailMap.values()) {
      if (entry.condition != PinDetailHandler.PinCondition.MINT) {
        return false; // At least one pin is not MINT
      }
    }

    // All pins are MINT - series is complete
    return true;
  }
}
