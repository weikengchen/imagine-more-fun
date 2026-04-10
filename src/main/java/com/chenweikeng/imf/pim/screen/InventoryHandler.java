package com.chenweikeng.imf.pim.screen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class InventoryHandler {
  private static InventoryHandler instance;
  private CacheResult lastCache;
  private long lastCacheTime;
  private static final long CACHE_TIMEOUT_MS = 2000;

  private InventoryHandler() {
    this.lastCache = null;
    this.lastCacheTime = 0;
  }

  public static InventoryHandler getInstance() {
    if (instance == null) {
      instance = new InventoryHandler();
    }
    return instance;
  }

  public List<String> recomputeCache(NonNullList<Slot> slots) {
    long currentTime = System.currentTimeMillis();
    if (lastCache != null && (currentTime - lastCacheTime) < CACHE_TIMEOUT_MS) {
      return new ArrayList<>(lastCache.missingMintPinNames);
    }

    Set<String> missingMintPinNames = new HashSet<>();
    Set<String> incompleteSeries = new HashSet<>();

    for (Slot slot : slots) {
      if (!(slot.container instanceof Inventory)) {
        continue;
      }

      ItemStack itemStack = slot.getItem();
      if (itemStack.isEmpty()) {
        continue;
      }

      PinDetailHandler.PinDetailEntry parsedEntry =
          PinDetailHandler.getInstance().parsePinEntry(itemStack);
      if (parsedEntry == null || parsedEntry.condition != PinDetailHandler.PinCondition.MINT) {
        continue;
      }

      String pinSeries = PinDetailHandler.getInstance().parsePinSeriesFromLore(itemStack);
      if (pinSeries == null) {
        continue;
      }

      PinDetailHandler.PinDetailEntry existingEntry =
          PinDetailHandler.getInstance().findDetailEntry(pinSeries, parsedEntry.pinName);
      if (existingEntry == null) {
        continue;
      }
      if (existingEntry != null && existingEntry.condition == PinDetailHandler.PinCondition.MINT) {
        continue;
      }

      missingMintPinNames.add(pinSeries + ":" + parsedEntry.pinName);
      incompleteSeries.add(pinSeries);
    }

    lastCache = new CacheResult(missingMintPinNames, incompleteSeries);
    lastCacheTime = currentTime;

    return new ArrayList<>(missingMintPinNames);
  }

  public Set<String> getIncompleteSeries(NonNullList<Slot> slots) {
    long currentTime = System.currentTimeMillis();
    if (lastCache != null && (currentTime - lastCacheTime) < CACHE_TIMEOUT_MS) {
      return new HashSet<>(lastCache.incompleteSeries);
    }

    recomputeCache(slots);

    return lastCache.incompleteSeries;
  }

  public Set<String> getMissingMintPinNames(NonNullList<Slot> slots) {
    long currentTime = System.currentTimeMillis();
    if (lastCache != null && (currentTime - lastCacheTime) < CACHE_TIMEOUT_MS) {
      return new HashSet<>(lastCache.missingMintPinNames);
    }

    recomputeCache(slots);

    return lastCache.missingMintPinNames;
  }

  private static class CacheResult {
    final Set<String> missingMintPinNames;
    final Set<String> incompleteSeries;

    CacheResult(Set<String> missingMintPinNames, Set<String> incompleteSeries) {
      this.missingMintPinNames = missingMintPinNames;
      this.incompleteSeries = incompleteSeries;
    }
  }
}
