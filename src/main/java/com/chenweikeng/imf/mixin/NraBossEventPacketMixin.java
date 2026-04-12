package com.chenweikeng.imf.mixin;

import com.chenweikeng.imf.nra.ServerState;
import com.chenweikeng.imf.nra.tracker.QuestTriangulationTracker;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept boss bar update packets for quest triangulation. When a boss bar name is
 * updated (containing distance), this extracts the distance for triangulation.
 */
@Mixin(ClientPacketListener.class)
public class NraBossEventPacketMixin {

  @Inject(at = @At("TAIL"), method = "handleBossUpdate")
  private void onHandleBossUpdate(ClientboundBossEventPacket packet, CallbackInfo ci) {
    if (!ServerState.isImagineFunServer()) {
      return;
    }

    // Dispatch the packet to extract name updates
    packet.dispatch(new BossBarDistanceHandler());
  }

  /** Handler that captures name updates and removals from boss bar packets. */
  private static class BossBarDistanceHandler implements ClientboundBossEventPacket.Handler {
    @Override
    public void add(
        UUID id,
        Component name,
        float progress,
        BossEvent.BossBarColor color,
        BossEvent.BossBarOverlay overlay,
        boolean darkenScreen,
        boolean playMusic,
        boolean createWorldFog) {
      // New boss bar added - check if it has distance
      String nameStr = name.getString();
      if (nameStr.contains("(") && nameStr.contains(")")) {
        QuestTriangulationTracker.getInstance().onBossBarUpdate(id, nameStr);
      }
    }

    @Override
    public void updateName(UUID id, Component name) {
      // Boss bar name updated - this is where distance changes
      String nameStr = name.getString();
      if (nameStr.contains("(") && nameStr.contains(")")) {
        QuestTriangulationTracker.getInstance().onBossBarUpdate(id, nameStr);
      }
    }

    @Override
    public void remove(UUID id) {
      // Boss bar removed - notify tracker
      QuestTriangulationTracker.getInstance().onBossBarRemoved(id);
    }
  }
}
