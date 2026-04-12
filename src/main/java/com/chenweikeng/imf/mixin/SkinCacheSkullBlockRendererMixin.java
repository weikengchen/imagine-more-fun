package com.chenweikeng.imf.mixin;

import com.chenweikeng.imf.skincache.SkinCacheMod;
import com.chenweikeng.imf.skincache.SkinCacheStats;
import com.chenweikeng.imf.skincache.cache.TextureCache;
import com.chenweikeng.imf.skincache.prewarm.ProfileTextureExtractor;
import com.chenweikeng.imf.skincache.prewarm.ProfileTextureExtractor.SkinInfo;
import com.chenweikeng.imf.skincache.prewarm.TextureRegistrar;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkullBlockRenderer.class)
public abstract class SkinCacheSkullBlockRendererMixin {

  // Used only when SkinCacheStats.DEBUG_PER_HASH is true. Static-final-false
  // dead-code-eliminates the call sites, so these stay empty in normal builds.
  private static final ConcurrentHashMap<String, Boolean> loggedKeys = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<String, Boolean> loggedMisses = new ConcurrentHashMap<>();

  @Inject(
      method =
          "resolveSkullRenderType(Lnet/minecraft/world/level/block/SkullBlock$Type;Lnet/minecraft/world/level/block/entity/SkullBlockEntity;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
      at = @At("HEAD"),
      cancellable = true)
  private void skincache$resolveFromCache(
      SkullBlock.Type type, SkullBlockEntity entity, CallbackInfoReturnable<RenderType> cir) {
    if (type != SkullBlock.Types.PLAYER) return;

    ResolvableProfile ownerProfile = entity.getOwnerProfile();
    if (ownerProfile == null) {
      SkinCacheStats.skullMissOther.incrementAndGet();
      if (SkinCacheStats.DEBUG_PER_HASH) logMiss("NO_PROFILE", "null");
      return;
    }

    // Extract texture URL directly from profile properties (no network call)
    SkinInfo skinInfo = ProfileTextureExtractor.extract(ownerProfile);
    if (skinInfo == null) {
      SkinCacheStats.skullMissOther.incrementAndGet();
      if (SkinCacheStats.DEBUG_PER_HASH) {
        logMiss("NO_TEXTURE_PROP", String.valueOf(ownerProfile.partialProfile().id()));
      }
      return;
    }

    if (!TextureCache.isCached(skinInfo.textureUrl())) {
      SkinCacheStats.skullMissNoPng.incrementAndGet();
      if (SkinCacheStats.DEBUG_PER_HASH) logMiss("NO_PNG", skinInfo.textureHash());
      return;
    }

    Identifier textureId = Identifier.withDefaultNamespace(skinInfo.textureIdPath());

    if (!TextureRegistrar.ensureRegistered(textureId, skinInfo.textureUrl())) {
      SkinCacheStats.skullMissOther.incrementAndGet();
      if (SkinCacheStats.DEBUG_PER_HASH) logMiss("REG_FAIL", skinInfo.textureHash());
      return;
    }

    SkinCacheStats.skullShortCircuit.incrementAndGet();
    if (SkinCacheStats.DEBUG_PER_HASH
        && loggedKeys.putIfAbsent(skinInfo.textureHash(), Boolean.TRUE) == null) {
      SkinCacheMod.log(
          "[SkullMixin] SHORT-CIRCUIT hash=" + skinInfo.textureHash() + " textureId=" + textureId);
    }
    cir.setReturnValue(RenderTypes.entityTranslucent(textureId));
  }

  private static void logMiss(String reason, String key) {
    String missKey = reason + ":" + key;
    if (loggedMisses.putIfAbsent(missKey, Boolean.TRUE) == null) {
      SkinCacheMod.log("[SkullMixin] MISS reason=" + reason + " key=" + key);
    }
  }
}
