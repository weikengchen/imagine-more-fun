package com.chenweikeng.imf.pim.trader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PinTraderRegistry {
  private static PinTraderRegistry instance;
  private final List<PinTrader> allTraders;
  private final Map<String, PinTraderLocation> locationsByWarpPoint;
  private final List<PinTraderLocation> allLocations;

  private PinTraderRegistry() {
    this.allTraders = new ArrayList<>();
    this.locationsByWarpPoint = new HashMap<>();
    this.allLocations = new ArrayList<>();
    initializeTraders();
  }

  public static PinTraderRegistry getInstance() {
    if (instance == null) {
      instance = new PinTraderRegistry();
    }
    return instance;
  }

  private void initializeTraders() {
    PinTraderLocation westward = addLocation("Westward Ho Trading Company", "pin");
    PinTraderLocation emp = addLocation("Emporium", "emp");
    PinTraderLocation tomorrowLanding = addLocation("TomorrowLanding", "tomorrowlanding");
    PinTraderLocation fiveAndDime = addLocation("Five & Dime", "fiveanddime");
    PinTraderLocation sanFransokyo = addLocation("Rita's Turbine Blenders", "sanfransokyo");
    PinTraderLocation courtOfAngels = addLocation("Court of Angels", "courtofangels");
    PinTraderLocation tsi = addLocation("Tom Sawyer Island", "tsi");
    PinTraderLocation toyDarianToyMaker = addLocation("Toydarian Toymaker", "toydariantoymaker");
    PinTraderLocation webSuppliers = addLocation("WEB Suppliers", "websuppliers");
    PinTraderLocation cozyCone = addLocation("Cozy Cone Motel", "cozycone");
    PinTraderLocation rushinRiverOutfitters =
        addLocation("Rushin' River Outfitters", "rushinriveroutfitters");
    PinTraderLocation runawayRailway =
        addLocation("Mickey & Minnie's Runaway Railway", "runawayrailway");
    PinTraderLocation bingBongs = addLocation("Bing Bong's Sweet Stuff", "bingbongs");

    westward.addTrader(new PinTrader(152, 64, 422, westward));
    westward.addTrader(new PinTrader(129, 64, 393, westward));
    westward.addTrader(new PinTrader(132, 64, 390, westward));

    emp.addTrader(new PinTrader(27, 64, 113, emp));
    tomorrowLanding.addTrader(new PinTrader(-330, 64, 410, tomorrowLanding));
    fiveAndDime.addTrader(new PinTrader(-38, 64, -383, fiveAndDime));
    sanFransokyo.addTrader(new PinTrader(179, 63, -896, sanFransokyo));
    courtOfAngels.addTrader(new PinTrader(453, 66, 169, courtOfAngels));
    tsi.addTrader(new PinTrader(536, 73, 603, tsi));
    toyDarianToyMaker.addTrader(new PinTrader(510, 68, 915, toyDarianToyMaker));
    webSuppliers.addTrader(new PinTrader(-83, 64, -760, webSuppliers));
    cozyCone.addTrader(new PinTrader(-32, 63, -870, cozyCone));
    rushinRiverOutfitters.addTrader(new PinTrader(337, 65, -592, rushinRiverOutfitters));
    runawayRailway.addTrader(new PinTrader(-118, 64, 1138, runawayRailway));
    bingBongs.addTrader(new PinTrader(594, 60, -1043, bingBongs));

    for (PinTraderLocation location : allLocations) {
      for (PinTrader trader : location.getTraders()) {
        allTraders.add(trader);
      }
    }
  }

  private PinTraderLocation addLocation(String name, String warpPoint) {
    PinTraderLocation location = new PinTraderLocation(name, warpPoint);
    locationsByWarpPoint.put(warpPoint, location);
    allLocations.add(location);
    return location;
  }

  public PinTrader getTraderByPosition(int position) {
    if (position < 0 || position >= allTraders.size()) {
      return null;
    }
    return allTraders.get(position);
  }

  public PinTraderLocation getLocationByWarpPoint(String warpPoint) {
    return locationsByWarpPoint.get(warpPoint);
  }

  public List<PinTrader> getTradersByWarpPoint(String warpPoint) {
    PinTraderLocation location = locationsByWarpPoint.get(warpPoint);
    return location != null ? location.getTraders() : new ArrayList<>();
  }

  public PinTrader getFirstTraderByWarpPoint(String warpPoint) {
    PinTraderLocation location = locationsByWarpPoint.get(warpPoint);
    return location != null ? location.getFirstTrader() : null;
  }

  public PinTraderLocation getLocationByName(String name) {
    for (PinTraderLocation location : allLocations) {
      if (location.getName().equalsIgnoreCase(name)) {
        return location;
      }
    }
    return null;
  }

  public List<PinTraderLocation> getAllLocations() {
    return new ArrayList<>(allLocations);
  }

  public List<PinTrader> getAllTraders() {
    return new ArrayList<>(allTraders);
  }

  public PinTrader getNextTrader(int currentPosition) {
    int nextPosition = currentPosition + 1;
    if (nextPosition >= allTraders.size()) {
      nextPosition = 0;
    }
    return getTraderByPosition(nextPosition);
  }

  public PinTrader getPreviousTrader(int currentPosition) {
    int prevPosition = currentPosition - 1;
    if (prevPosition < 0) {
      prevPosition = allTraders.size() - 1;
    }
    return getTraderByPosition(prevPosition);
  }

  public int getNextDistinctWarpPointPosition(int currentPosition) {
    if (currentPosition < 0 || currentPosition >= allTraders.size()) {
      return -1;
    }
    PinTrader currentTrader = allTraders.get(currentPosition);
    PinTraderLocation currentLocation = currentTrader.getLocation();
    for (int i = currentPosition + 1; i < allTraders.size(); i++) {
      PinTrader trader = allTraders.get(i);
      if (trader.getLocation() != currentLocation) {
        return i;
      }
    }
    return -1;
  }

  public int getTraderCount() {
    return allTraders.size();
  }

  public int getLocationCount() {
    return allLocations.size();
  }

  public boolean hasLocation(String warpPoint) {
    return locationsByWarpPoint.containsKey(warpPoint);
  }

  public int getWarpPointIndex(String warpPoint) {
    PinTraderLocation location = locationsByWarpPoint.get(warpPoint);
    if (location == null) {
      return -1;
    }

    for (int i = 0; i < allLocations.size(); i++) {
      if (allLocations.get(i) == location) {
        return i + 1;
      }
    }
    return -1;
  }

  public String getOrdinalNumber(int n) {
    if (n < 1) {
      return String.valueOf(n);
    }

    String suffix;
    int lastTwo = n % 100;
    int lastOne = n % 10;

    if (lastTwo >= 11 && lastTwo <= 13) {
      suffix = "th";
    } else {
      switch (lastOne) {
        case 1:
          suffix = "st";
          break;
        case 2:
          suffix = "nd";
          break;
        case 3:
          suffix = "rd";
          break;
        default:
          suffix = "th";
          break;
      }
    }
    return n + suffix;
  }

  public int getLocationIndex(PinTraderLocation location) {
    for (int i = 0; i < allLocations.size(); i++) {
      if (allLocations.get(i) == location) {
        return i;
      }
    }
    return -1;
  }

  public PinTraderLocation getNextLocation(PinTraderLocation currentLocation) {
    int index = getLocationIndex(currentLocation);
    if (index == -1 || index + 1 >= allLocations.size()) {
      return null;
    }
    return allLocations.get(index + 1);
  }

  public PinTraderLocation getFirstLocation() {
    if (allLocations.isEmpty()) {
      return null;
    }
    return allLocations.get(0);
  }
}
