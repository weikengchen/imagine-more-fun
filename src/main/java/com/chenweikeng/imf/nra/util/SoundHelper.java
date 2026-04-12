package com.chenweikeng.imf.nra.util;

import com.chenweikeng.imf.nra.GameState;
import com.chenweikeng.imf.nra.compat.MonkeycraftCompat;
import com.chenweikeng.imf.nra.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class SoundHelper {
  private static final String NOTIFICATION_TITLE = "ImagineMoreFun";
  private static final String NOTIFICATION_BODY = "You are not riding!";

  public static void playConfiguredSound(Minecraft client) {
    if (client.player == null || client.level == null) {
      return;
    }

    if (GameState.getInstance().isMonkeyAttached()) {
      MonkeycraftCompat.sendImmediateNotification(NOTIFICATION_TITLE, NOTIFICATION_BODY, true);
      return;
    }

    String soundId = ModConfig.currentSetting.soundId;
    Identifier soundIdentifier = Identifier.parse(soundId);

    if (client.getSoundManager().getSoundEvent(soundIdentifier) == null) {
      soundIdentifier =
          Identifier.fromNamespaceAndPath("minecraft", "entity.experience_orb.pickup");
    }

    client.level.playSound(
        client.player,
        client.player.getX(),
        client.player.getY(),
        client.player.getZ(),
        SoundEvent.createVariableRangeEvent(soundIdentifier),
        SoundSource.MASTER,
        1.0f,
        1.0f);
  }
}
