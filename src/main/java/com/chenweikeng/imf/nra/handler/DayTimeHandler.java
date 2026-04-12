package com.chenweikeng.imf.nra.handler;

import com.chenweikeng.imf.nra.GameState;
import com.chenweikeng.imf.nra.ServerState;
import com.chenweikeng.imf.nra.config.FullbrightMode;
import com.chenweikeng.imf.nra.config.ModConfig;
import com.chenweikeng.imf.nra.tracker.QuestTriangulationTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public class DayTimeHandler {
  private static final long NOON = 6000L;
  private static final long SUNSET_START = 12000L;
  private static final long MIDNIGHT = 18000L;
  private static final long SUNRISE_END = 1000L;

  public void resetDayTimeIfNeeded(Minecraft client) {
    if (!ServerState.isImagineFunServer()) {
      return;
    }

    if (FireworkViewingHandler.getInstance().isViewingFirework()) {
      return;
    }

    ClientLevel level = client.level;
    if (level == null) {
      return;
    }

    // Quest tracking takes priority - set to night for beam visibility
    if (QuestTriangulationTracker.getInstance().hasConfidentEstimate()) {
      long time = level.getDayTime() % 24000L;
      // Set to midnight if it's daytime (between sunrise end and sunset start)
      if (time >= SUNRISE_END && time < SUNSET_START) {
        level.getLevelData().setDayTime(MIDNIGHT);
      }
      return;
    }

    // Normal fullbright logic
    boolean isRiding = GameState.getInstance().isRiding();
    FullbrightMode mode = ModConfig.currentSetting.fullbrightMode;
    boolean shouldApplyFullbright =
        switch (mode) {
          case NONE -> false;
          case ONLY_WHEN_RIDING -> isRiding;
          case ONLY_WHEN_NOT_RIDING -> !isRiding;
          case ALWAYS -> true;
        };

    if (!shouldApplyFullbright) {
      return;
    }

    long time = level.getDayTime() % 24000L;

    if (time >= SUNSET_START) {
      level.getLevelData().setDayTime(NOON);
    }
  }
}
