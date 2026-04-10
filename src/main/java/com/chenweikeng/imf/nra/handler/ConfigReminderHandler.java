package com.chenweikeng.imf.nra.handler;

import com.chenweikeng.imf.nra.ServerState;
import com.chenweikeng.imf.nra.wizard.TutorialManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

public class ConfigReminderHandler {
  private static final int INITIAL_DELAY_TICKS = 600;
  private static final int REMINDER_INTERVAL_TICKS = 12000;

  private boolean hasShownInitialReminder = false;
  private long lastReminderTick = 0;

  public void track(Minecraft client, long currentTick) {
    if (!ServerState.isImagineFunServer()) {
      return;
    }

    if (TutorialManager.getInstance().isCompletedForCurrentVersion()) {
      return;
    }

    if (client.player == null) {
      return;
    }

    if (!hasShownInitialReminder) {
      if (currentTick >= INITIAL_DELAY_TICKS) {
        sendReminder(client);
        hasShownInitialReminder = true;
        lastReminderTick = currentTick;
      }
    } else {
      if (currentTick - lastReminderTick >= REMINDER_INTERVAL_TICKS) {
        sendReminder(client);
        lastReminderTick = currentTick;
      }
    }
  }

  private void sendReminder(Minecraft client) {
    if (client.player == null) {
      return;
    }

    Component message =
        Component.empty()
            .withStyle(ChatFormatting.AQUA)
            .append(
                Component.literal("[NRA] ")
                    .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD))
            .append(
                Component.literal(
                        "This mod modifies your gameplay experience significantly. To set up the mod, please run ")
                    .withStyle(ChatFormatting.WHITE))
            .append(
                Component.literal("/nra setup")
                    .withStyle(
                        s ->
                            s.withColor(ChatFormatting.AQUA)
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent.RunCommand("nra setup"))))
            .append(
                Component.literal(" to open the setup wizard.").withStyle(ChatFormatting.WHITE));

    client.player.displayClientMessage(message, false);
  }

  public void reset() {
    hasShownInitialReminder = false;
    lastReminderTick = 0;
  }
}
