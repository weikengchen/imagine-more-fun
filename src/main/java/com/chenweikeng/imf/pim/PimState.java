package com.chenweikeng.imf.pim;

import com.chenweikeng.imf.pim.trader.PinTrader;
import com.chenweikeng.imf.pim.trader.PinTraderRegistry;
import java.util.List;

public class PimState {
  private static boolean enabled = false;
  private static int currentTraderPosition = 0;
  private static String currentWarpPoint = null;

  public static boolean isEnabled() {
    return enabled;
  }

  public static void setEnabled(boolean value) {
    enabled = value;
  }

  public static int getCurrentTraderPosition() {
    return currentTraderPosition;
  }

  public static void resetWarpPoint() {
    currentTraderPosition = 0;
    currentWarpPoint = null;
  }

  public static String getCurrentWarpPoint() {
    PinTrader trader = PinTraderRegistry.getInstance().getTraderByPosition(currentTraderPosition);
    if (trader == null) {
      return null;
    }
    return trader.getLocation().getWarpPoint();
  }

  public static String getActiveWarpPoint() {
    return currentWarpPoint;
  }

  public static void setActiveWarpPoint(String warpPoint) {
    currentWarpPoint = warpPoint;
  }

  public static boolean hasNextWarpPoint() {
    return currentTraderPosition < PinTraderRegistry.getInstance().getTraderCount();
  }

  public static void incrementWarpPoint() {
    int nextPos =
        PinTraderRegistry.getInstance().getNextDistinctWarpPointPosition(currentTraderPosition);
    if (nextPos == -1) {
      currentTraderPosition = PinTraderRegistry.getInstance().getTraderCount();
    } else {
      currentTraderPosition = nextPos;
    }
  }

  public static List<PinTrader> getTraders() {
    return PinTraderRegistry.getInstance().getAllTraders();
  }
}
