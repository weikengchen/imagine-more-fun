package com.chenweikeng.imf.pim.pin;

public class PinPackColor {
  public final Color color;
  public final int price;

  private PinPackColor(Color color, int price) {
    this.color = color;
    this.price = price;
  }

  public enum Color {
    BLUE,
    PINK,
    GREEN,
    YELLOW,
    UNKNOWN
  }

  public static PinPackColor BLUE = new PinPackColor(Color.BLUE, 400);
  public static PinPackColor PINK = new PinPackColor(Color.PINK, 500);
  public static PinPackColor GREEN = new PinPackColor(Color.GREEN, 100);
  public static PinPackColor YELLOW = new PinPackColor(Color.YELLOW, 400);

  public static PinPackColor fromString(String colorCode) {
    if (colorCode == null || colorCode.isEmpty()) {
      return null;
    }

    String lowerCode = colorCode.toLowerCase();
    if (lowerCode.contains("blue")) {
      return BLUE;
    } else if (lowerCode.contains("light_purple")) {
      return PINK;
    } else if (lowerCode.contains("green")) {
      return GREEN;
    } else if (lowerCode.contains("yellow")) {
      return YELLOW;
    }

    return null;
  }
}
