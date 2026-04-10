package com.chenweikeng.imf.nra.handler;

import com.chenweikeng.imf.nra.GameState;
import com.chenweikeng.imf.nra.ServerState;
import com.chenweikeng.imf.nra.config.FullbrightMode;
import com.chenweikeng.imf.nra.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public class DayTimeHandler {
  private static final long NOON = 6000L;
  private static final long SUNSET_START = 12000L;

  public void resetDayTimeIfNeeded(Minecraft client) {
    if (!ServerState.isImagineFunServer()) {
      return;
    }

    if (FireworkViewingHandler.getInstance().isViewingFirework()) {
      return;
    }

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

    ClientLevel level = client.level;
    if (level == null) {
      return;
    }

    long time = level.getDayTime() % 24000L;

    if (time >= SUNSET_START) {
      level.getLevelData().setDayTime(NOON);
    }
  }
}
