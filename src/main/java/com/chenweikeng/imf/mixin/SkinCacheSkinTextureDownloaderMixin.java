package com.chenweikeng.imf.mixin;

import com.chenweikeng.imf.skincache.SkinCacheMod;
import com.chenweikeng.imf.skincache.cache.TextureCache;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkinTextureDownloader.class)
public abstract class SkinCacheSkinTextureDownloaderMixin {

  @Inject(
      method =
          "downloadSkin(Ljava/nio/file/Path;Ljava/lang/String;)Lcom/mojang/blaze3d/platform/NativeImage;",
      at = @At("HEAD"),
      cancellable = true)
  private void skincache$onDownloadSkin(
      Path localCopy, String url, CallbackInfoReturnable<NativeImage> cir) {
    try {
      var cached = TextureCache.get(url);
      if (cached.isPresent()) {
        byte[] data = Files.readAllBytes(cached.get());
        cir.setReturnValue(NativeImage.read(data));
        return;
      }
    } catch (IOException e) {
      SkinCacheMod.LOGGER.warn("[SkinCache] Error reading cache, falling through to download", e);
    }
  }

  @Inject(
      method =
          "downloadSkin(Ljava/nio/file/Path;Ljava/lang/String;)Lcom/mojang/blaze3d/platform/NativeImage;",
      at = @At("RETURN"))
  private void skincache$afterDownloadSkin(
      Path localCopy, String url, CallbackInfoReturnable<NativeImage> cir) {
    if (TextureCache.isCached(url)) return;

    try {
      if (Files.isRegularFile(localCopy)) {
        byte[] data = Files.readAllBytes(localCopy);
        if (!TextureCache.put(url, data)) {
          SkinCacheMod.LOGGER.warn("[SkinCache] REJECTED (invalid) url={}", url);
        }
      }
    } catch (IOException e) {
      SkinCacheMod.LOGGER.error("[SkinCache] Failed to cache downloaded texture", e);
    }
  }
}
