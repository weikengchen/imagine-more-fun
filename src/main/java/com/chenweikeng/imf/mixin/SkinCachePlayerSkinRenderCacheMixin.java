package com.chenweikeng.imf.mixin;

import net.minecraft.client.renderer.PlayerSkinRenderCache;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Placeholder — debug logging removed. The SkullBlockRendererMixin now handles the player head
 * short-circuit directly.
 */
@Mixin(PlayerSkinRenderCache.class)
public abstract class SkinCachePlayerSkinRenderCacheMixin {}
