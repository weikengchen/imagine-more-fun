package com.chenweikeng.imf.mixin;

import com.chenweikeng.imf.nra.ServerState;
import com.chenweikeng.imf.nra.tracker.QuestTriangulationTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to track outgoing position packets for quest triangulation. Records positions when they are
 * sent to the server, allowing correlation with boss bar distance updates.
 */
@Mixin(LocalPlayer.class)
public class NraLocalPlayerPositionMixin {

  @Inject(at = @At("HEAD"), method = "sendPosition")
  private void onSendPosition(CallbackInfo ci) {
    if (!ServerState.isImagineFunServer()) {
      return;
    }

    LocalPlayer player = (LocalPlayer) (Object) this;
    Vec3 position = player.position();
    QuestTriangulationTracker.getInstance().recordSentPosition(position);
  }
}
