package com.chenweikeng.imf.mixin;

import com.chenweikeng.imf.skincache.SkinCacheMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks world join for logging. Texture registration is done on-demand
 * by the render-path mixins via TextureRegistrar.ensureRegistered().
 */
@Mixin(Minecraft.class)
public abstract class SkinCacheMinecraftMixin {

    @Inject(method = "setLevel", at = @At("RETURN"))
    private void skincache$onWorldJoin(ClientLevel level, CallbackInfo ci) {
        SkinCacheMod.log("[SkinCacheMinecraftMixin] World joined — textures will be registered on-demand");
    }
}
