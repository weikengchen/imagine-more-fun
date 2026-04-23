package com.chenweikeng.imf.nra.dailyplan;

import com.chenweikeng.imf.mixin.NraBossHealthOverlayAccessor;
import com.chenweikeng.imf.nra.ServerState;
import com.chenweikeng.imf.nra.config.ModConfig;
import com.chenweikeng.imf.nra.config.TrackerDisplayMode;
import com.chenweikeng.imf.nra.ride.AutograbHolder;
import com.chenweikeng.imf.nra.ride.CurrentRideHolder;
import com.chenweikeng.imf.nra.ride.RideCountManager;
import com.chenweikeng.imf.nra.ride.RideName;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;

public final class DailyPlanHudRenderer {
  private static final int COLOR_TITLE = 0xFFFFD700;
  private static final int COLOR_DIM = 0xFFAAAAAA;
  private static final int COLOR_DONE = 0xFF55FF55;
  private static final int COLOR_PROGRESS = 0xFFFFAA00;
  private static final int COLOR_IDLE = 0xFFFFFFFF;
  private static final int COLOR_IDLE_GLYPH = 0xFF888888;
  private static final int COLOR_CONNECTOR = 0xFF555555;

  private static final int PANEL_BG = 0xB0000000;
  private static final int NODE_BG = 0x60000000;

  private static final String GLYPH_DONE = "\u25CF";
  private static final String GLYPH_PROGRESS = "\u25D0";
  private static final String GLYPH_IDLE = "\u25CB";

  private static final int FONT_HEIGHT = 9;
  private static final int NODE_H_PAD = 4;
  private static final int NODE_V_PAD = 3;
  private static final int NODE_HEIGHT = FONT_HEIGHT * 2 + NODE_V_PAD * 2 + 1;
  private static final int CONNECTOR_WIDTH = 14;
  private static final int PANEL_H_PAD = 6;
  private static final int PANEL_TOP_PAD = 3;
  private static final int PANEL_BOTTOM_PAD = 4;

  private DailyPlanHudRenderer() {}

  public static boolean isActive() {
    if (!ModConfig.currentSetting.showDailyPlanHud) {
      return false;
    }
    if (!ServerState.isImagineFunServer()) {
      return false;
    }
    DailyPlan plan = DailyPlanManager.getInstance().getOrCreateToday();
    return plan != null && plan.nodes != null && !plan.nodes.isEmpty();
  }

  public static void render(GuiGraphics context, DeltaTracker tickCounter) {
    if (!ModConfig.currentSetting.showDailyPlanHud) {
      return;
    }
    if (!ServerState.isImagineFunServer()) {
      return;
    }

    Minecraft client = Minecraft.getInstance();
    if (client == null || client.gui == null || client.player == null || client.font == null) {
      return;
    }

    TrackerDisplayMode mode = ModConfig.currentSetting.trackerDisplayMode;
    if (mode == TrackerDisplayMode.NEVER) {
      return;
    }
    boolean isRiding =
        CurrentRideHolder.getCurrentRide() != null
            || AutograbHolder.getRideAtLocation(client) != null;
    if (mode == TrackerDisplayMode.ONLY_WHEN_RIDING && !isRiding) {
      return;
    }
    if (mode == TrackerDisplayMode.ONLY_WHEN_NOT_RIDING && isRiding) {
      return;
    }

    BossHealthOverlay bossOverlay = client.gui.getBossOverlay();
    Map<UUID, LerpingBossEvent> bossEvents =
        ((NraBossHealthOverlayAccessor) bossOverlay).getEvents();
    if (bossEvents != null && !bossEvents.isEmpty()) {
      return;
    }

    DailyPlan plan = DailyPlanManager.getInstance().getOrCreateToday();
    if (plan == null || plan.nodes == null || plan.nodes.isEmpty()) {
      return;
    }

    renderPlan(context, client.font, client, plan);
  }

  private static void renderPlan(GuiGraphics context, Font font, Minecraft client, DailyPlan plan) {
    int screenWidth = client.getWindow().getGuiScaledWidth();

    int done = 0;
    for (DailyPlanNode n : plan.nodes) {
      if (n.completed) {
        done++;
      }
    }
    int total = plan.nodes.size();
    boolean allDone = done == total;

    String title =
        "\u2728 Ride Plan \u00B7 "
            + formatDateFriendly(plan.date)
            + " \u00B7 "
            + done
            + "/"
            + total;
    int titleColor = allDone ? COLOR_DONE : COLOR_TITLE;
    int titleWidth = font.width(title);

    List<NodeLayout> layouts = buildLayouts(font, plan);

    int chainWidth = 0;
    for (int i = 0; i < layouts.size(); i++) {
      chainWidth += layouts.get(i).width;
      if (i < layouts.size() - 1) {
        chainWidth += CONNECTOR_WIDTH;
      }
    }

    int panelInnerWidth = Math.max(chainWidth, titleWidth);
    int panelWidth = panelInnerWidth + PANEL_H_PAD * 2;
    int panelHeight = PANEL_TOP_PAD + FONT_HEIGHT + 3 + NODE_HEIGHT + PANEL_BOTTOM_PAD;
    int panelX = (screenWidth - panelWidth) / 2;
    int panelY = 0;

    context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_BG);

    int titleX = (screenWidth - titleWidth) / 2;
    int titleY = panelY + PANEL_TOP_PAD;
    context.drawString(font, title, titleX, titleY, titleColor, false);

    int chainStartX = (screenWidth - chainWidth) / 2;
    int boxTopY = titleY + FONT_HEIGHT + 3;
    int boxCenterY = boxTopY + NODE_HEIGHT / 2;

    int x = chainStartX;
    for (int i = 0; i < layouts.size(); i++) {
      NodeLayout layout = layouts.get(i);
      drawNodeBox(context, font, layout, x, boxTopY);
      x += layout.width;

      if (i < layouts.size() - 1) {
        int connColor = layout.isDone ? COLOR_DONE : COLOR_CONNECTOR;
        drawConnector(context, x, boxCenterY, x + CONNECTOR_WIDTH, connColor);
        x += CONNECTOR_WIDTH;
      }
    }
  }

  private static List<NodeLayout> buildLayouts(Font font, DailyPlan plan) {
    RideCountManager counts = RideCountManager.getInstance();
    List<NodeLayout> out = new ArrayList<>(plan.nodes.size());

    for (DailyPlanNode node : plan.nodes) {
      RideName ride = RideName.fromMatchString(node.ride);

      Integer snap = plan.snapshotCounts == null ? null : plan.snapshotCounts.get(node.ride);
      int baseline = snap == null ? 0 : snap;
      int delta = Math.max(0, counts.getRideCount(ride) - baseline);
      int progress = Math.min(delta, node.k);

      NodeLayout layout = new NodeLayout();
      layout.isDone = node.completed;
      boolean isPartial = !layout.isDone && progress > 0;

      if (layout.isDone) {
        layout.glyph = GLYPH_DONE;
        layout.glyphColor = COLOR_DONE;
        layout.nameColor = COLOR_DONE;
        layout.progColor = COLOR_DONE;
        layout.borderColor = COLOR_DONE;
      } else if (isPartial) {
        layout.glyph = GLYPH_PROGRESS;
        layout.glyphColor = COLOR_PROGRESS;
        layout.nameColor = COLOR_IDLE;
        layout.progColor = COLOR_PROGRESS;
        layout.borderColor = COLOR_PROGRESS;
      } else {
        layout.glyph = GLYPH_IDLE;
        layout.glyphColor = COLOR_IDLE_GLYPH;
        layout.nameColor = COLOR_IDLE;
        layout.progColor = COLOR_DIM;
        layout.borderColor = COLOR_IDLE_GLYPH;
      }

      layout.name = ride.getShortName().toUpperCase(Locale.ENGLISH);
      layout.prog = layout.isDone ? ("\u00D7" + node.k) : (progress + "/" + node.k);

      String topRow = layout.glyph + " " + layout.name;
      int topWidth = font.width(topRow);
      int botWidth = font.width(layout.prog);
      layout.width = Math.max(topWidth, botWidth) + NODE_H_PAD * 2;
      layout.topRow = topRow;
      layout.topRowWidth = topWidth;
      layout.botRowWidth = botWidth;
      layout.glyphWidth = font.width(layout.glyph + " ");

      out.add(layout);
    }

    return out;
  }

  private static void drawNodeBox(
      GuiGraphics context, Font font, NodeLayout layout, int left, int top) {
    int right = left + layout.width;
    int bottom = top + NODE_HEIGHT;

    context.fill(left, top, right, bottom, NODE_BG);
    context.hLine(left, right - 1, top, layout.borderColor);
    context.hLine(left, right - 1, bottom - 1, layout.borderColor);
    context.vLine(left, top, bottom - 1, layout.borderColor);
    context.vLine(right - 1, top, bottom - 1, layout.borderColor);

    int topRowStartX = left + (layout.width - layout.topRowWidth) / 2;
    int topRowY = top + NODE_V_PAD;
    context.drawString(font, layout.glyph + " ", topRowStartX, topRowY, layout.glyphColor, false);
    context.drawString(
        font, layout.name, topRowStartX + layout.glyphWidth, topRowY, layout.nameColor, false);

    int botRowX = left + (layout.width - layout.botRowWidth) / 2;
    int botRowY = topRowY + FONT_HEIGHT + 1;
    context.drawString(font, layout.prog, botRowX, botRowY, layout.progColor, false);
  }

  private static void drawConnector(
      GuiGraphics context, int left, int centerY, int right, int color) {
    context.hLine(left, right - 1, centerY - 1, color);
    context.hLine(left, right - 1, centerY, color);
  }

  private static String formatDateFriendly(String dateStr) {
    try {
      LocalDate d = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
      return d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
          + " "
          + d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
          + " "
          + d.getDayOfMonth();
    } catch (Exception e) {
      return dateStr;
    }
  }

  private static final class NodeLayout {
    String glyph;
    int glyphColor;
    String name;
    int nameColor;
    String prog;
    int progColor;
    int borderColor;
    boolean isDone;
    int width;
    String topRow;
    int topRowWidth;
    int botRowWidth;
    int glyphWidth;
  }
}
