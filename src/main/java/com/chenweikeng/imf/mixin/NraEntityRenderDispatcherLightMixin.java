package com.chenweikeng.imf.mixin;

import com.chenweikeng.imf.nra.spacemountain.SpaceMountainOverride;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forces armor-stand entities to render at {@link LightTexture#FULL_BRIGHT} while the player is
 * riding Space or Hyperspace Mountain.
 *
 * <p>The dome we built has near-zero block-light and zero sky-light, so the vanilla lightmap
 * multiply leaves the show effects (TIE Fighters, X-Wings, etc.) barely visible. The server renders
 * those ships as armor stands wearing custom-modelled diamond swords in the helmet slot, so
 * flipping just armor stands to full-bright lights up exactly the show props without touching the
 * dark terrain or other rider entities.
 *
 * <p>The player's own vehicle is also an armor stand. It's normally invisible, so brightening it is
 * a no-op visually. If a non-show armor stand (e.g. boarding-station decoration) gets caught in the
 * override, it'll also render bright — acceptable trade-off given how rare that is inside the dome.
 */
@Mixin(EntityRenderDispatcher.class)
public class NraEntityRenderDispatcherLightMixin {
  @Inject(method = "getPackedLightCoords", at = @At("HEAD"), cancellable = true)
  private void imf$bypassLightmapForRide(
      Entity entity, float partialTickTime, CallbackInfoReturnable<Integer> cir) {
    if (SpaceMountainOverride.isActive() && entity instanceof ArmorStand) {
      cir.setReturnValue(LightTexture.FULL_BRIGHT);
    }
  }
}
