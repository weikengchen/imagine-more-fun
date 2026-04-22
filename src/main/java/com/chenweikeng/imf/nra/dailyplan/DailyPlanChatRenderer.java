package com.chenweikeng.imf.nra.dailyplan;

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

  private DailyPlanChatRenderer() {}

  public static void send(Minecraft client, DailyPlan plan) {
    if (client == null || client.player == null || plan == null) {
      return;
    }

    sendLine(client, Component.literal(DIVIDER).withColor(HEADER_COLOR));
    sendLine(
        client,
        Component.empty()
            .append(Component.literal("\u2728 ").withColor(HEADER_COLOR))
            .append(
                Component.literal("Today's Ride Plan")
                    .withStyle(ChatFormatting.BOLD)
                    .withColor(HEADER_COLOR))
            .append(
                Component.literal("  " + formatDateFriendly(plan.date)).withColor(INDEX_COLOR)));

    if (plan.nodes == null || plan.nodes.isEmpty()) {
      sendLine(
          client,
          Component.literal("No eligible rides — you may have hit your max goal on everything.")
              .withColor(EMPTY_COLOR));
      sendLine(client, Component.literal(DIVIDER).withColor(HEADER_COLOR));
      return;
    }

    for (int i = 0; i < plan.nodes.size(); i++) {
      DailyPlanNode node = plan.nodes.get(i);
      String displayName = RideName.fromMatchString(node.ride).getDisplayName();
      sendLine(
          client,
          Component.empty()
              .append(Component.literal(" " + (i + 1) + ". \u25CB ").withColor(INDEX_COLOR))
              .append(Component.literal(displayName).withColor(RIDE_COLOR))
              .append(
                  Component.literal(" \u00D7" + node.k)
                      .withStyle(ChatFormatting.BOLD)
                      .withColor(K_COLOR)));
    }

    sendLine(client, Component.literal(DIVIDER).withColor(HEADER_COLOR));
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
