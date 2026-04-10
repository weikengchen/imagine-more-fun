package com.chenweikeng.imf.nra.session;

import com.chenweikeng.imf.nra.util.SoundHelper;
import com.chenweikeng.imf.nra.util.TimeFormatUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class MilestoneHandler {
  private static final int[] MILESTONES = {10, 25, 50, 100, 150, 200, 250, 500, 1000};

  public static void checkMilestone(int ridesCompleted, long totalRideTimeSeconds) {
    for (int milestone : MILESTONES) {
      if (ridesCompleted == milestone) {
        celebrate(milestone, totalRideTimeSeconds);
        return;
      }
    }
  }

  private static void celebrate(int milestone, long totalRideTimeSeconds) {
    Minecraft client = Minecraft.getInstance();
    if (client.player == null) {
      return;
    }

    SoundHelper.playConfiguredSound(client);

    String timeStr = TimeFormatUtil.formatDuration(totalRideTimeSeconds);
    Component message =
        Component.empty()
            .append(
                Component.literal("[NRA] ")
                    .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD))
            .append(
                Component.literal("Milestone: " + milestone + " rides completed today!")
                    .withStyle(ChatFormatting.GOLD))
            .append(
                Component.literal(" Total ride time: " + timeStr).withStyle(ChatFormatting.YELLOW));

    client.player.displayClientMessage(message, false);
  }
}
