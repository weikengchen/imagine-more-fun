package com.chenweikeng.imf.pim.pin;

import java.util.HashMap;
import java.util.Map;

public enum Rarity {
  SIGNATURE,
  DELUXE,
  RARE,
  UNCOMMON,
  COMMON;

  private static final Map<String, Rarity> LOOKUP_MAP = new HashMap<>();

  static {
    for (Rarity rarity : values()) {
      LOOKUP_MAP.put(rarity.name().toLowerCase(), rarity);
    }
  }

  public static Rarity fromString(String text) {
    if (text == null) {
      return COMMON;
    }
    Rarity rarity = LOOKUP_MAP.get(text.toLowerCase());
    return rarity != null ? rarity : COMMON;
  }
}
