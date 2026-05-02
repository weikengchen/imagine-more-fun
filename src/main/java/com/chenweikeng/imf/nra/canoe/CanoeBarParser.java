package com.chenweikeng.imf.nra.canoe;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Parses the canoe speed bar text from the action-bar {@link Component}.
 *
 * <p>The server sends a styled component shaped like:
 *
 * <pre>
 *   "" (empty parent)
 *     "3.1 "    color=#48DBFB   (cyan, the current speed value)
 *     "["       color=#C8D6E5   (light gray)
 *     "檡檡檡檡" color=#32FF7E   (green, filled bar segments)
 *     "檡檡]"   color=#C8D6E5   (gray, empty bar segments + close bracket)
 * </pre>
 *
 * <p>The bar holds 16 segments total. Each segment is {@code U+6AA1} ('檡'), interleaved with
 * invisible {@code U+F001} kerning glyphs from a custom resource-pack font.
 */
public final class CanoeBarParser {

  /** Hex color used for the "filled" portion of the bar. */
  public static final int FILL_COLOR = 0x32FF7E;

  /** The bar segment glyph (U+6AA1, '檡'). */
  public static final int SEGMENT_CODEPOINT = 0x6AA1;

  /** Number of bar segments at full speed. */
  public static final int TOTAL_SEGMENTS = 16;

  /** Maximum boat speed. */
  public static final float MAX_SPEED = 3.5f;

  /** Result of parsing the action-bar component. */
  public static final class Parsed {
    /** Raw plain-text contents of the component (for logging). */
    public final String raw;

    /** Parsed speed value (e.g. {@code 3.1f}), or {@code Float.NaN} if not detected. */
    public final float speed;

    /** Number of green-colored bar segments (filled). {@code -1} if no bar present. */
    public final int fill;

    /** Total number of bar segments seen. {@code -1} if no bar present. */
    public final int total;

    Parsed(String raw, float speed, int fill, int total) {
      this.raw = raw;
      this.speed = speed;
      this.fill = fill;
      this.total = total;
    }

    /** True when this component looks like a canoe speed bar (has both speed and bar). */
    public boolean isCanoeBar() {
      return !Float.isNaN(speed) && total > 0;
    }
  }

  private CanoeBarParser() {}

  /** Parse the given component. Never returns null. */
  public static Parsed parse(Component component) {
    if (component == null) {
      return new Parsed("", Float.NaN, -1, -1);
    }
    String raw = component.getString();

    float[] speedHolder = {Float.NaN};
    int[] counts = {0, 0}; // [fill, total]

    component.visit(
        (style, text) -> {
          if (text == null || text.isEmpty()) return java.util.Optional.empty();
          // Speed prefix: "3.1 " — purely digits/dot/space, contains a dot
          String trimmed = text.trim();
          if (Float.isNaN(speedHolder[0]) && looksLikeSpeed(trimmed)) {
            try {
              speedHolder[0] = Float.parseFloat(trimmed);
            } catch (NumberFormatException ignored) {
              // not a speed
            }
          }
          // Count segment glyphs and how many of them carry the green fill color.
          int segs = countSegments(text);
          if (segs > 0) {
            counts[1] += segs;
            if (isFillColor(style)) {
              counts[0] += segs;
            }
          }
          return java.util.Optional.empty();
        },
        Style.EMPTY);

    return new Parsed(
        raw,
        speedHolder[0],
        counts[0] == 0 && counts[1] == 0 ? -1 : counts[0],
        counts[1] == 0 ? -1 : counts[1]);
  }

  private static boolean looksLikeSpeed(String s) {
    if (s.isEmpty()) return false;
    boolean sawDigit = false;
    boolean sawDot = false;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c >= '0' && c <= '9') sawDigit = true;
      else if (c == '.') {
        if (sawDot) return false;
        sawDot = true;
      } else return false;
    }
    return sawDigit && sawDot;
  }

  private static int countSegments(String text) {
    int n = 0;
    for (int i = 0; i < text.length(); ) {
      int cp = text.codePointAt(i);
      if (cp == SEGMENT_CODEPOINT) n++;
      i += Character.charCount(cp);
    }
    return n;
  }

  private static boolean isFillColor(Style style) {
    if (style == null) return false;
    TextColor color = style.getColor();
    return color != null && (color.getValue() & 0xFFFFFF) == FILL_COLOR;
  }
}
