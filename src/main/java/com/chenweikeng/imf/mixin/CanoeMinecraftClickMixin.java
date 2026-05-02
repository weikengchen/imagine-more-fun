package com.chenweikeng.imf.mixin;

import com.chenweikeng.imf.nra.canoe.CanoeHelperClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Logs every left/right click while the canoe helper is active. The helper itself filters by
 * paddle-held state, so this mixin just forwards.
 */
@Mixin(Minecraft.class)
public class CanoeMinecraftClickMixin {

  @Inject(at = @At("HEAD"), method = "startUseItem")
  private void imf$canoeOnStartUseItem(CallbackInfo ci) {
    CanoeHelperClient.get().onClick(true);
  }

  @Inject(at = @At("HEAD"), method = "startAttack")
  private void imf$canoeOnStartAttack(CallbackInfoReturnable<Boolean> ci) {
    CanoeHelperClient.get().onClick(false);
  }
}
