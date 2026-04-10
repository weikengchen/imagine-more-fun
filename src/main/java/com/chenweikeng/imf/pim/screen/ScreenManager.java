package com.chenweikeng.imf.pim.screen;

import net.minecraft.client.gui.screens.Screen;

public class ScreenManager {
  public enum ScreenType {
    PIN_RARITY_WINDOW,
    PIN_PINBOOK_WINDOW,
    PIN_TRADER_BOARD,
    UNKNOWN
  }

  private static final String PIN_RARITY_MARKER = "\u4e52";
  private static final String PIN_PINBOOK_MARKER = "\u4e51";
  private static final String PIN_TRADER_BOARD_MARKER_1 = "\u4e54";
  private static final String PIN_TRADER_BOARD_MARKER_2 = "\u9dfc";

  public static ScreenType detectScreenType(Screen screen) {
    if (screen == null) {
      return ScreenType.UNKNOWN;
    }

    String title = screen.getTitle().getString();

    if (title.contains(PIN_RARITY_MARKER)) {
      return ScreenType.PIN_RARITY_WINDOW;
    } else if (title.contains(PIN_PINBOOK_MARKER)) {
      return ScreenType.PIN_PINBOOK_WINDOW;
    } else if (title.contains(PIN_TRADER_BOARD_MARKER_1)
        && title.contains(PIN_TRADER_BOARD_MARKER_2)) {
      return ScreenType.PIN_TRADER_BOARD;
    }

    return ScreenType.UNKNOWN;
  }
}
