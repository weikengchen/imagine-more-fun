package com.chenweikeng.imf.nra.dailyplan.ui;

import com.chenweikeng.imf.nra.dailyplan.DailyPlan;
import com.chenweikeng.imf.nra.dailyplan.DailyPlanLayer;
import com.chenweikeng.imf.nra.dailyplan.DailyPlanLayer.LayerType;
import com.chenweikeng.imf.nra.dailyplan.DailyPlanManager;
import com.chenweikeng.imf.nra.dailyplan.DailyPlanNode;
import com.chenweikeng.imf.nra.ride.CurrentRideHolder;
import com.chenweikeng.imf.nra.ride.RideCountManager;
import com.chenweikeng.imf.nra.ride.RideName;
import com.chenweikeng.imf.nra.util.TimeFormatUtil;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Full-window view of today's plan, scrollable, opened by `/rideplan open` or the J keybind. */
public class DailyPlanScreen extends Screen {
  private static final int COLOR_TITLE = 0xFFFFD700;
  private static final int COLOR_DIM = 0xFFAAAAAA;
  private static final int COLOR_DONE = 0xFF55FF55;
  private static final int COLOR_PROGRESS = 0xFFFFAA00;
  private static final int COLOR_IDLE = 0xFFFFFFFF;
  private static final int COLOR_IDLE_GLYPH = 0xFF888888;
  private static final int COLOR_BADGE = 0xFFBB88FF;
  private static final int COLOR_ACTIVE_HIGHLIGHT = 0xFFFFFFFF;
  private static final int COLOR_SEPARATOR = 0x44FFFFFF;

  private static final int PANEL_BG = 0xCC000000;
  private static final int NODE_BG = 0x60000000;
  private static final int ACTIVE_ROW_BG = 0x30FFFFFF;

  private static final String GLYPH_DONE = "\u25CF";
  private static final String GLYPH_PROGRESS = "\u25D0";
  private static final String GLYPH_IDLE = "\u25CB";
  private static final String ARROW_ACTIVE = "\u25B6";
  private static final String CHECK = "\u2713";

  private static final int FONT_HEIGHT = 9;
  private static final int NODE_H_PAD = 5;
  private static final int NODE_V_PAD = 3;
  private static final int NODE_HEIGHT = FONT_HEIGHT * 2 + NODE_V_PAD * 2 + 1;
  private static final int NODE_GAP = 6;
  private static final int ROW_PADDING = 6;
  private static final int ROW_HEIGHT = NODE_HEIGHT + ROW_PADDING * 2;

  private static final int PANEL_MARGIN_X = 48;
  private static final int PANEL_MARGIN_TOP = 28;
  private static final int PANEL_MARGIN_BOTTOM = 40;
  private static final int HEADER_HEIGHT = FONT_HEIGHT + 6 + FONT_HEIGHT + 6;

  private final Screen parent;
  private int scrollOffset = 0;
  private int maxScroll = 0;

  public DailyPlanScreen(Screen parent) {
    super(Component.literal("Daily Ride Plan"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    super.init();
    int btnW = 80;
    int btnH = 20;
    int btnX = (this.width - btnW) / 2;
    int btnY = this.height - PANEL_MARGIN_BOTTOM + (PANEL_MARGIN_BOTTOM - btnH) / 2;
    this.addRenderableWidget(
        Button.builder(Component.literal("Close"), btn -> onClose())
            .bounds(btnX, btnY, btnW, btnH)
            .build());
  }

  @Override
  public void onClose() {
    if (this.minecraft != null) {
      this.minecraft.setScreen(parent);
    }
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    super.render(graphics, mouseX, mouseY, delta);

    DailyPlan plan = DailyPlanManager.getInstance().getOrCreateToday();
    if (plan == null || plan.layers == null) {
      graphics.drawCenteredString(
          this.font,
          Component.literal("No ride plan available."),
          this.width / 2,
          this.height / 2,
          COLOR_DIM);
      return;
    }

    int panelLeft = PANEL_MARGIN_X;
    int panelRight = this.width - PANEL_MARGIN_X;
    int panelTop = PANEL_MARGIN_TOP;
    int panelBottom = this.height - PANEL_MARGIN_BOTTOM;

    graphics.fill(panelLeft, panelTop, panelRight, panelBottom, PANEL_BG);

    renderHeader(graphics, plan, panelLeft, panelTop, panelRight);

    int listTop = panelTop + HEADER_HEIGHT + 4;
    int listBottom = panelBottom - 4;
    int visibleHeight = listBottom - listTop;

    graphics.enableScissor(panelLeft + 2, listTop, panelRight - 2, listBottom);

    int activeIdx = firstIncompleteIndex(plan);

    int y = listTop - scrollOffset;
    for (int i = 0; i < plan.layers.size(); i++) {
      int rowBottom = y + ROW_HEIGHT;
      if (rowBottom >= listTop && y <= listBottom) {
        renderLayerRow(
            graphics,
            plan,
            i,
            plan.layers.get(i),
            panelLeft + 10,
            y,
            panelRight - 10,
            i == activeIdx);
      }
      y += ROW_HEIGHT;
    }

    graphics.disableScissor();

    int totalContentHeight = plan.layers.size() * ROW_HEIGHT;
    maxScroll = Math.max(0, totalContentHeight - visibleHeight);
    scrollOffset = Math.min(scrollOffset, maxScroll);

    renderScrollHints(graphics, panelLeft, listTop, panelRight, listBottom);
  }

  private static int firstIncompleteIndex(DailyPlan plan) {
    for (int i = 0; i < plan.layers.size(); i++) {
      if (!plan.layers.get(i).completed) {
        return i;
      }
    }
    return -1;
  }

  private void renderHeader(GuiGraphics graphics, DailyPlan plan, int left, int top, int right) {
    int centerX = (left + right) / 2;

    int activeLevel = plan.layers.size() + 1;
    for (int i = 0; i < plan.layers.size(); i++) {
      if (!plan.layers.get(i).completed) {
        activeLevel = i + 1;
        break;
      }
    }
    String titleLine =
        "\u2728 Ride Plan  \u00B7  "
            + formatDateFriendly(plan.date)
            + "  \u00B7  Level "
            + activeLevel;
    graphics.drawCenteredString(
        this.font, Component.literal(titleLine), centerX, top + 4, COLOR_TITLE);

    String subLine = buildSubLine();
    int subColor = COLOR_DIM;
    if (CurrentRideHolder.getCurrentRide() != null) {
      subColor = 0xFF55FF88;
    }
    graphics.drawCenteredString(
        this.font, Component.literal(subLine), centerX, top + 4 + FONT_HEIGHT + 4, subColor);

    int sepY = top + HEADER_HEIGHT - 1;
    graphics.fill(left + 4, sepY, right - 4, sepY + 1, COLOR_SEPARATOR);
  }

  private static String buildSubLine() {
    RideName current = CurrentRideHolder.getCurrentRide();
    if (current != null) {
      Integer progress = CurrentRideHolder.getCurrentProgressPercent();
      Integer elapsed = CurrentRideHolder.getElapsedSeconds();
      StringBuilder sb = new StringBuilder("\u25B6 ").append(current.getDisplayName());
      if (progress != null && elapsed != null) {
        int remaining = Math.max(0, current.getRideTime() - elapsed);
        sb.append(" \u00B7 ")
            .append(progress)
            .append("% \u00B7 ")
            .append(TimeFormatUtil.formatDuration(remaining))
            .append(" left");
      }
      return sb.toString();
    }
    return "Scroll to browse. ESC to close.";
  }

  private void renderLayerRow(
      GuiGraphics graphics,
      DailyPlan plan,
      int layerIdx,
      DailyPlanLayer layer,
      int left,
      int top,
      int right,
      boolean isActive) {
    if (isActive) {
      graphics.fill(left - 6, top, right + 6, top + ROW_HEIGHT, ACTIVE_ROW_BG);
    }

    int inner = top + ROW_PADDING;

    String layerLabel = "Layer " + (layerIdx + 1);
    int layerLabelColor =
        layer.completed ? COLOR_DONE : (isActive ? COLOR_ACTIVE_HIGHLIGHT : COLOR_DIM);
    graphics.drawString(
        this.font,
        layerLabel,
        left,
        inner + (NODE_HEIGHT - FONT_HEIGHT) / 2,
        layerLabelColor,
        false);
    int labelWidth = this.font.width("Layer 999");

    int statusX = left + labelWidth + 6;
    String statusGlyph = layer.completed ? CHECK : (isActive ? ARROW_ACTIVE : "\u00B7");
    int statusColor = layer.completed ? COLOR_DONE : (isActive ? COLOR_PROGRESS : COLOR_IDLE_GLYPH);
    graphics.drawString(
        this.font,
        statusGlyph,
        statusX,
        inner + (NODE_HEIGHT - FONT_HEIGHT) / 2,
        statusColor,
        false);

    int nodesX = statusX + this.font.width(statusGlyph + "  ") + 4;

    if (layer.type != LayerType.SINGLE) {
      String badge = "[" + layer.type.badge() + "]";
      graphics.drawString(
          this.font, badge, nodesX, inner + (NODE_HEIGHT - FONT_HEIGHT) / 2, COLOR_BADGE, false);
      nodesX += this.font.width(badge) + 6;
    }

    RideCountManager counts = RideCountManager.getInstance();
    int x = nodesX;
    for (DailyPlanNode node : layer.nodes) {
      x = drawNodeBox(graphics, plan, counts, node, x, inner);
      x += NODE_GAP;
    }
  }

  private int drawNodeBox(
      GuiGraphics graphics,
      DailyPlan plan,
      RideCountManager counts,
      DailyPlanNode node,
      int left,
      int top) {
    RideName ride = RideName.fromMatchString(node.ride);
    Integer snap = plan.snapshotCounts == null ? null : plan.snapshotCounts.get(node.ride);
    int baseline = snap == null ? 0 : snap;
    int delta = Math.max(0, counts.getRideCount(ride) - baseline);
    int progress = Math.min(delta, node.k);

    String glyph;
    int glyphColor;
    int nameColor;
    int progColor;
    int borderColor;
    if (node.completed) {
      glyph = GLYPH_DONE;
      glyphColor = COLOR_DONE;
      nameColor = COLOR_DONE;
      progColor = COLOR_DONE;
      borderColor = COLOR_DONE;
    } else if (progress > 0) {
      glyph = GLYPH_PROGRESS;
      glyphColor = COLOR_PROGRESS;
      nameColor = COLOR_IDLE;
      progColor = COLOR_PROGRESS;
      borderColor = COLOR_PROGRESS;
    } else {
      glyph = GLYPH_IDLE;
      glyphColor = COLOR_IDLE_GLYPH;
      nameColor = COLOR_IDLE;
      progColor = COLOR_DIM;
      borderColor = COLOR_IDLE_GLYPH;
    }

    String displayName = ride.getDisplayName();
    String progText = node.completed ? ("\u00D7" + node.k) : (progress + "/" + node.k);

    String topLine = glyph + " " + displayName;
    int topWidth = this.font.width(topLine);
    int botWidth = this.font.width(progText);
    int boxWidth = Math.max(topWidth, botWidth) + NODE_H_PAD * 2;
    int boxRight = left + boxWidth;
    int boxBottom = top + NODE_HEIGHT;

    graphics.fill(left, top, boxRight, boxBottom, NODE_BG);
    graphics.hLine(left, boxRight - 1, top, borderColor);
    graphics.hLine(left, boxRight - 1, boxBottom - 1, borderColor);
    graphics.vLine(left, top, boxBottom - 1, borderColor);
    graphics.vLine(boxRight - 1, top, boxBottom - 1, borderColor);

    int topTextX = left + (boxWidth - topWidth) / 2;
    int topTextY = top + NODE_V_PAD;
    int glyphWidth = this.font.width(glyph + " ");
    graphics.drawString(this.font, glyph + " ", topTextX, topTextY, glyphColor, false);
    graphics.drawString(this.font, displayName, topTextX + glyphWidth, topTextY, nameColor, false);

    int botTextX = left + (boxWidth - botWidth) / 2;
    int botTextY = topTextY + FONT_HEIGHT + 1;
    graphics.drawString(this.font, progText, botTextX, botTextY, progColor, false);

    return boxRight;
  }

  private void renderScrollHints(GuiGraphics graphics, int left, int top, int right, int bottom) {
    if (scrollOffset > 0) {
      graphics.drawCenteredString(
          this.font, Component.literal("\u25B2"), (left + right) / 2, top - 2, COLOR_DIM);
    }
    if (scrollOffset < maxScroll) {
      graphics.drawCenteredString(
          this.font, Component.literal("\u25BC"), (left + right) / 2, bottom - 6, COLOR_DIM);
    }
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
    scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (dy * 16)));
    return true;
  }

  @Override
  public boolean keyPressed(KeyEvent event) {
    if (super.keyPressed(event)) {
      return true;
    }
    int keyCode = event.key();
    // Down arrow / Page Down scroll down; Up / Page Up scroll up
    if (keyCode == 264 || keyCode == 267) { // DOWN, PAGE_DOWN
      int step = keyCode == 267 ? ROW_HEIGHT * 4 : ROW_HEIGHT;
      scrollOffset = Math.min(maxScroll, scrollOffset + step);
      return true;
    }
    if (keyCode == 265 || keyCode == 266) { // UP, PAGE_UP
      int step = keyCode == 266 ? ROW_HEIGHT * 4 : ROW_HEIGHT;
      scrollOffset = Math.max(0, scrollOffset - step);
      return true;
    }
    return false;
  }

  private static String formatDateFriendly(String dateStr) {
    try {
      LocalDate d = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
      return d.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
          + ", "
          + d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
          + " "
          + d.getDayOfMonth()
          + ", "
          + d.getYear();
    } catch (Exception e) {
      return dateStr;
    }
  }
}
