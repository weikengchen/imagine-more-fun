package com.chenweikeng.imf.mixin;

import com.chenweikeng.imf.nra.NotRidingAlertClient;
import com.chenweikeng.imf.nra.tracker.OtherPlayerStatsTracker;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class NraClientPacketListenerCommandMixin {

  @Inject(at = @At("HEAD"), method = "sendCommand")
  private void nra$onSendCommand(String command, CallbackInfo ci) {
    if (!NotRidingAlertClient.isImagineFunServer()) {
      return;
    }

    String trimmed = command.trim();
    if (trimmed.equalsIgnoreCase("ridestats")) {
      OtherPlayerStatsTracker.getInstance().setRideStatsActive(true);
    }
  }
}
