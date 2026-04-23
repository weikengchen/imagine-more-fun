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
  private static final int COLOR_CONNECTOR = 0xFF666666;

  private static final String GLYPH_DONE = "\u25CF";
  private static final String GLYPH_PROGRESS = "\u25D0";
  private static final String GLYPH_IDLE = "\u25CB";
  private static final String CONNECTOR = " \u2500\u2500 ";

  private static final int TOP_PADDING = 2;
  private static final int LINE_HEIGHT = 10;

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
    int titleX = (screenWidth - titleWidth) / 2;
    context.drawString(font, title, titleX, TOP_PADDING, titleColor, false);

    RideCountManager counts = RideCountManager.getInstance();
    List<NodeSegment> segments = new ArrayList<>(plan.nodes.size());
    int totalChainWidth = 0;

    for (int i = 0; i < plan.nodes.size(); i++) {
      DailyPlanNode node = plan.nodes.get(i);
      RideName ride = RideName.fromMatchString(node.ride);

      Integer snap = plan.snapshotCounts == null ? null : plan.snapshotCounts.get(node.ride);
      int baseline = snap == null ? 0 : snap;
      int delta = Math.max(0, counts.getRideCount(ride) - baseline);
      int progress = Math.min(delta, node.k);

      String glyph;
      int glyphColor;
      int textColor;
      if (node.completed) {
        glyph = GLYPH_DONE;
        glyphColor = COLOR_DONE;
        textColor = COLOR_DONE;
      } else if (progress > 0) {
        glyph = GLYPH_PROGRESS;
        glyphColor = COLOR_PROGRESS;
        textColor = COLOR_IDLE;
      } else {
        glyph = GLYPH_IDLE;
        glyphColor = COLOR_IDLE_GLYPH;
        textColor = COLOR_IDLE;
      }

      String name = ride.getShortName().toUpperCase(Locale.ENGLISH);
      String prog = node.completed ? ("\u00D7" + node.k) : (progress + "/" + node.k);

      int glyphWidth = font.width(glyph + " ");
      int nameWidth = font.width(name + " ");
      int progWidth = font.width(prog);

      NodeSegment seg = new NodeSegment();
      seg.glyph = glyph;
      seg.glyphColor = glyphColor;
      seg.text = name + " " + prog;
      seg.textColor = textColor;
      seg.progressColor = node.completed ? COLOR_DONE : (progress > 0 ? COLOR_PROGRESS : COLOR_DIM);
      seg.name = name;
      seg.prog = prog;
      seg.width = glyphWidth + nameWidth + progWidth;

      segments.add(seg);
      totalChainWidth += seg.width;
      if (i < plan.nodes.size() - 1) {
        totalChainWidth += font.width(CONNECTOR);
      }
    }

    int chainX = (screenWidth - totalChainWidth) / 2;
    int chainY = TOP_PADDING + LINE_HEIGHT + 2;

    int x = chainX;
    for (int i = 0; i < segments.size(); i++) {
      NodeSegment seg = segments.get(i);
      context.drawString(font, seg.glyph + " ", x, chainY, seg.glyphColor, false);
      x += font.width(seg.glyph + " ");
      context.drawString(font, seg.name + " ", x, chainY, seg.textColor, false);
      x += font.width(seg.name + " ");
      context.drawString(font, seg.prog, x, chainY, seg.progressColor, false);
      x += font.width(seg.prog);
      if (i < segments.size() - 1) {
        context.drawString(font, CONNECTOR, x, chainY, COLOR_CONNECTOR, false);
        x += font.width(CONNECTOR);
      }
    }
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

  private static final class NodeSegment {
    String glyph;
    int glyphColor;
    String text;
    int textColor;
    int progressColor;
    String name;
    String prog;
    int width;
  }
}
