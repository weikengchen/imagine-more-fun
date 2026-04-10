package com.chenweikeng.imf.nra.config;

public enum MaxGoal {
  K1(1000),
  K5(5000),
  K10(10000);

  private final int value;

  private static final MaxGoal[] VALUES = values();

  MaxGoal(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public String getDisplayName() {
    return switch (this) {
      case K1 -> "1K";
      case K5 -> "5K";
      case K10 -> "10K";
    };
  }

  public String getDisplayValue() {
    return String.valueOf(value);
  }

  public MaxGoal previous() {
    int currentIndex = ordinal();
    // Don't wrap: stay at first element if trying to go before it
    if (currentIndex == 0) {
      return VALUES[0];
    }
    return VALUES[currentIndex - 1];
  }

  public MaxGoal next() {
    int currentIndex = ordinal();
    // Don't wrap: stay at last element if trying to go beyond it
    if (currentIndex == VALUES.length - 1) {
      return VALUES[VALUES.length - 1];
    }
    return VALUES[currentIndex + 1];
  }
}
