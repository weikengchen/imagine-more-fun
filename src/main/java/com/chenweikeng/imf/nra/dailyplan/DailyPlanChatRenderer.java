package com.chenweikeng.imf.nra.dailyplan;

import com.chenweikeng.imf.nra.dailyplan.DailyPlanLayer.LayerType;
import com.chenweikeng.imf.nra.ride.RideCountManager;
import com.chenweikeng.imf.nra.ride.RideName;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class DailyPlanChatRenderer {
  private static final String DIVIDER = "━━━━━━━━━━━━━━━━━━━━━━━━━━";
  private static final int HEADER_COLOR = 0xFFFFD700;
  private static final int RIDE_COLOR = 0xFFFFFFFF;
  private static final int K_COLOR = 0xFF55FFFF;
  private static final int INDEX_COLOR = 0xFFAAAAAA;
  private static final int EMPTY_COLOR = 0xFFFFAA00;
  private static final int DONE_COLOR = 0xFF55FF55;
  private static final int PROGRESS_COLOR = 0xFFFFAA00;
  private static final int BADGE_COLOR = 0xFFBB88FF;

  private DailyPlanChatRenderer() {}

  public static void send(Minecraft client, DailyPlan plan) {
    if (client == null || client.player == null || plan == null) {
      return;
    }

    int totalLayers = plan.layers == null ? 0 : plan.layers.size();
    int activeLevel = totalLayers + 1;
    if (plan.layers != null) {
      for (int i = 0; i < plan.layers.size(); i++) {
        if (!plan.layers.get(i).completed) {
          activeLevel = i + 1;
          break;
        }
      }
    }

    sendLine(client, Component.literal(DIVIDER).withColor(HEADER_COLOR));
    Component header =
        Component.empty()
            .append(Component.literal("\u2728 ").withColor(HEADER_COLOR))
            .append(
                Component.literal("Today's Ride Plan")
                    .withStyle(ChatFormatting.BOLD)
                    .withColor(HEADER_COLOR))
            .append(Component.literal("  " + formatDateFriendly(plan.date)).withColor(INDEX_COLOR));
    if (totalLayers > 0) {
      header =
          header
              .copy()
              .append(
                  Component.literal("  Level " + activeLevel)
                      .withStyle(ChatFormatting.BOLD)
                      .withColor(PROGRESS_COLOR));
    }
    sendLine(client, header);

    if (plan.layers == null || plan.layers.isEmpty()) {
      sendLine(
          client,
          Component.literal("No eligible rides — you may have hit your max goal on everything.")
              .withColor(EMPTY_COLOR));
      sendLine(client, Component.literal(DIVIDER).withColor(HEADER_COLOR));
      return;
    }

    RideCountManager counts = RideCountManager.getInstance();
    for (int i = 0; i < plan.layers.size(); i++) {
      DailyPlanLayer layer = plan.layers.get(i);
      renderLayer(client, counts, plan, i, layer);
    }

    sendLine(client, Component.literal(DIVIDER).withColor(HEADER_COLOR));
  }

  private static void renderLayer(
      Minecraft client, RideCountManager counts, DailyPlan plan, int index, DailyPlanLayer layer) {
    String indexStr = " " + (index + 1) + ". ";

    if (layer.type == LayerType.SINGLE) {
      DailyPlanNode node = layer.nodes.get(0);
      sendLine(client, buildNodeLine(indexStr, node, plan, counts));
      return;
    }

    String badge = "[" + layer.type.badge() + "]";
    Component header =
        Component.empty()
            .append(Component.literal(indexStr).withColor(INDEX_COLOR))
            .append(Component.literal(badge).withStyle(ChatFormatting.BOLD).withColor(BADGE_COLOR));
    if (layer.completed) {
      header =
          header
              .copy()
              .append(
                  Component.literal(" \u2713")
                      .withStyle(ChatFormatting.BOLD)
                      .withColor(DONE_COLOR));
    }
    sendLine(client, header);
    for (DailyPlanNode node : layer.nodes) {
      sendLine(client, buildNodeLine("      ", node, plan, counts));
    }
  }

  private static Component buildNodeLine(
      String prefix, DailyPlanNode node, DailyPlan plan, RideCountManager counts) {
    RideName ride = RideName.fromMatchString(node.ride);
    Integer snap = plan.snapshotCounts == null ? null : plan.snapshotCounts.get(node.ride);
    int baseline = snap == null ? 0 : snap;
    int delta = Math.max(0, counts.getRideCount(ride) - baseline);
    int progress = Math.min(delta, node.k);

    String glyph;
    int glyphColor;
    int nameColor;
    if (node.completed) {
      glyph = "\u25CF";
      glyphColor = DONE_COLOR;
      nameColor = DONE_COLOR;
    } else if (progress > 0) {
      glyph = "\u25D0";
      glyphColor = PROGRESS_COLOR;
      nameColor = RIDE_COLOR;
    } else {
      glyph = "\u25CB";
      glyphColor = INDEX_COLOR;
      nameColor = RIDE_COLOR;
    }

    Component progressBadge =
        node.completed
            ? Component.literal(" \u00D7" + node.k)
                .withStyle(ChatFormatting.BOLD)
                .withColor(DONE_COLOR)
            : Component.literal(" " + progress + "/" + node.k)
                .withStyle(ChatFormatting.BOLD)
                .withColor(progress > 0 ? PROGRESS_COLOR : K_COLOR);

    return Component.empty()
        .append(Component.literal(prefix).withColor(INDEX_COLOR))
        .append(Component.literal(glyph + " ").withColor(glyphColor))
        .append(Component.literal(ride.getDisplayName()).withColor(nameColor))
        .append(progressBadge);
  }

  private static void sendLine(Minecraft client, Component component) {
    client.player.displayClientMessage(component, false);
  }

  private static String formatDateFriendly(String dateStr) {
    try {
      LocalDate d = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
      return d.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
          + ", "
          + d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
          + " "
          + d.getDayOfMonth();
    } catch (Exception e) {
      return dateStr;
    }
  }
}
