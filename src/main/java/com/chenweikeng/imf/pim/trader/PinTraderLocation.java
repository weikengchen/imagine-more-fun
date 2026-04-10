package com.chenweikeng.imf.pim.trader;

import java.util.ArrayList;
import java.util.List;

public class PinTraderLocation {
  private final String name;
  private final String warpPoint;
  private final List<PinTrader> traders;

  public PinTraderLocation(String name, String warpPoint) {
    this.name = name;
    this.warpPoint = warpPoint;
    this.traders = new ArrayList<>();
  }

  public void addTrader(PinTrader trader) {
    traders.add(trader);
  }

  public String getName() {
    return name;
  }

  public String getWarpPoint() {
    return warpPoint;
  }

  public List<PinTrader> getTraders() {
    return new ArrayList<>(traders);
  }

  public PinTrader getFirstTrader() {
    return traders.isEmpty() ? null : traders.get(0);
  }

  public String getWarpCommand() {
    return "/warp " + warpPoint;
  }
}
