package com.chenweikeng.imf.mixin;

import com.chenweikeng.imf.nra.GameState;
import com.chenweikeng.imf.nra.ride.AutograbHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rubber band enforcement: when the player is inside an autograb zone and the zone-entry minimize
 * has fired, keep them from walking back out of the zone while the window is minimized.
 *
 * <p>Injects at HEAD of {@code sendPosition()}, which runs right after {@code super.tick()} has
 * already moved the player based on input this tick but <em>before</em> the movement packet is
 * dispatched. If the post-movement position is outside the autograb polygon, we snap the player
 * back to the last in-zone anchor so the packet carries that anchor to the server.
 *
 * <p>Three layers of safety gate this:
 *
 * <ol>
 *   <li>{@link GameState#isRubberBandActive()} — CursorManager must have explicitly armed it (which
 *       requires {@code showAutograbRegions=true}, a just-fired zone-entry minimize, and a valid
 *       player)
 *   <li>{@link AutograbHolder#getRideAtLocation} — the mixin only snaps when the player has
 *       actually stepped <em>outside</em> the polygon; movement inside the zone is untouched
 *   <li>Tick-based expiry (handled in CursorManager) — the band releases 1s after zone entry, on
 *       vehicle mount, on autograb failure, and on reset
 * </ol>
 */
@Mixin(LocalPlayer.class)
public class NraLocalPlayerMixin {
  @Inject(method = "sendPosition", at = @At("HEAD"))
  private void nra$enforceRubberBand(CallbackInfo ci) {
    GameState state = GameState.getInstance();
    if (!state.isRubberBandActive()) {
      return;
    }

    Minecraft client = Minecraft.getInstance();
    if (client == null || client.player == null) {
      return;
    }

    // Only snap when the player has actually stepped outside the autograb
    // polygon. If they're still inside, leave their movement alone.
    if (AutograbHolder.getRideAtLocation(client) != null) {
      return;
    }

    LocalPlayer self = (LocalPlayer) (Object) this;
    Vec3 delta = self.getDeltaMovement();
    self.setDeltaMovement(0, delta.y, 0);
    self.setPos(state.getRubberBandX(), state.getRubberBandY(), state.getRubberBandZ());
  }
}
