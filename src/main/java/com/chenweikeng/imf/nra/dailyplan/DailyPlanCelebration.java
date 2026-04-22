package com.chenweikeng.imf.nra.dailyplan;

import com.chenweikeng.imf.nra.ride.RideName;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public final class DailyPlanCelebration {
  private static final Random RANDOM = new Random();
  private static final int PARTICLE_COUNT = 12;

  private DailyPlanCelebration() {}

  public static void nodeCompleted(Minecraft client, RideName ride, int nodeIndex, int totalNodes) {
    if (client == null || client.player == null || client.level == null) {
      return;
    }

    Component msg =
        Component.empty()
            .append(Component.literal("\u2728 ").withStyle(ChatFormatting.GOLD))
            .append(Component.literal("[IMF] ").withStyle(ChatFormatting.YELLOW))
            .append(
                Component.literal("Node complete! ")
                    .withStyle(ChatFormatting.BOLD)
                    .withStyle(ChatFormatting.GREEN))
            .append(Component.literal(ride.getDisplayName()).withStyle(ChatFormatting.AQUA))
            .append(
                Component.literal(" (" + (nodeIndex + 1) + "/" + totalNodes + ")")
                    .withStyle(ChatFormatting.GRAY));
    client.player.displayClientMessage(msg, false);

    client
        .getSoundManager()
        .play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.5f, 1.0f));

    spawnParticles(client);
  }

  public static void planCompleted(Minecraft client) {
    if (client == null || client.player == null || client.level == null) {
      return;
    }

    Component msg =
        Component.empty()
            .append(Component.literal("\u2728 ").withStyle(ChatFormatting.GOLD))
            .append(Component.literal("[IMF] ").withStyle(ChatFormatting.YELLOW))
            .append(
                Component.literal("Daily Ride Plan complete!")
                    .withStyle(ChatFormatting.BOLD)
                    .withStyle(ChatFormatting.GOLD));
    client.player.displayClientMessage(msg, false);

    client
        .getSoundManager()
        .play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.2f));

    spawnParticles(client);
    spawnParticles(client);
  }

  private static void spawnParticles(Minecraft client) {
    double x = client.player.getX();
    double y = client.player.getY() + 1.0;
    double z = client.player.getZ();
    for (int i = 0; i < PARTICLE_COUNT; i++) {
      double dx = (RANDOM.nextDouble() - 0.5) * 1.4;
      double dy = RANDOM.nextDouble() * 0.8;
      double dz = (RANDOM.nextDouble() - 0.5) * 1.4;
      client.level.addParticle(ParticleTypes.HAPPY_VILLAGER, x + dx, y + dy, z + dz, 0, 0.1, 0);
    }
  }
}
