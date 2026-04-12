package com.chenweikeng.imf.skincache.prewarm;

import com.chenweikeng.imf.skincache.SkinCacheMod;
import com.chenweikeng.imf.skincache.cache.TextureCache;
import com.mojang.blaze3d.platform.NativeImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ResolvableProfile;

/**
 * Pre-warms player head skins when chunks load.
 *
 * <p>Strategy: 1. For each skull profile in the chunk, check if we have BOTH: - ProfileCache entry
 * (UUID → texture URL + hash + texture ID path) - TextureCache entry (URL → PNG bytes on disk) 2.
 * If yes: synchronously register the texture with TextureManager on the main thread (we're already
 * on it during chunk load). This means the GPU texture exists BEFORE the renderer ever calls
 * getOrDefault(). 3. Then call lookup() to populate PlayerSkinRenderCache — since the GPU texture
 * is already registered, the async chain completes instantly. 4. If no profile cache hit: fall back
 * to async lookup() (original behavior).
 */
public final class PrewarmRegistry {

  private static final ConcurrentHashMap<String, Set<ResolvableProfile>> chunkProfiles =
      new ConcurrentHashMap<>();

  private PrewarmRegistry() {}

  /**
   * Pre-warm skull profiles from a loaded chunk. Called on the main thread from
   * ClientChunkCacheMixin.
   */
  public static void prewarmChunk(
      String worldName,
      int chunkX,
      int chunkZ,
      java.util.List<ResolvableProfile> profiles,
      PlayerSkinRenderCache skinRenderCache) {
    if (profiles.isEmpty()) return;

    String key = chunkKey(worldName, chunkX, chunkZ);
    Set<ResolvableProfile> set = ConcurrentHashMap.newKeySet();
    set.addAll(profiles);
    chunkProfiles.put(key, set);

    int syncCount = 0;
    int asyncCount = 0;

    TextureManager textureManager = Minecraft.getInstance().getTextureManager();

    for (ResolvableProfile profile : profiles) {
      UUID uuid = profile.partialProfile().id();
      if (uuid == null) {
        // No UUID — can't look up profile cache, fall back to async
        skinRenderCache.lookup(profile);
        asyncCount++;
        continue;
      }

      ProfileCache.ProfileEntry entry = ProfileCache.get(uuid.toString());
      if (entry == null) {
        // No cached profile — async
        skinRenderCache.lookup(profile);
        asyncCount++;
        continue;
      }

      // Check if PNG is in our texture cache
      Optional<Path> cachedPng = TextureCache.get(entry.textureUrl);
      if (cachedPng.isEmpty()) {
        skinRenderCache.lookup(profile);
        asyncCount++;
        continue;
      }

      // We have everything! Register the texture synchronously.
      // Register texture synchronously on main thread — GPU upload happens now
      Identifier textureId = Identifier.withDefaultNamespace(entry.textureIdPath);
      try {
        byte[] pngData = Files.readAllBytes(cachedPng.get());
        NativeImage image = NativeImage.read(pngData);
        image = processLegacySkinIfNeeded(image);
        DynamicTexture texture = new DynamicTexture(textureId::toString, image);
        textureManager.register(textureId, texture);
        SkinCacheMod.LOGGER.debug(
            "[SkinCache] SYNC registered texture for UUID={} id={}", uuid, textureId);
      } catch (Exception e) {
        SkinCacheMod.LOGGER.warn(
            "[SkinCache] Failed to sync-register texture for UUID={}", uuid, e);
        skinRenderCache.lookup(profile);
        asyncCount++;
        continue;
      }

      // Kick off the lookup — since the texture is now registered, the async chain
      // should complete very quickly (profile resolve uses our cached data,
      // downloadSkin hits our cache, registerTextureInManager finds it already registered)
      skinRenderCache.lookup(profile);
      syncCount++;
    }

    if (syncCount > 0 || asyncCount > 0) {
      SkinCacheMod.LOGGER.debug(
          "[SkinCache] Pre-warmed chunk [{}, {}]: {} sync, {} async",
          chunkX,
          chunkZ,
          syncCount,
          asyncCount);
    }
  }

  /**
   * Replicate SkinTextureDownloader.processLegacySkin for 32px tall legacy skins. 64x64 skins pass
   * through unchanged.
   */
  private static NativeImage processLegacySkinIfNeeded(NativeImage image) {
    int w = image.getWidth();
    int h = image.getHeight();
    if (w == 64 && h == 32) {
      // Legacy skin — expand to 64x64
      NativeImage newImage = new NativeImage(64, 64, true);
      newImage.copyFrom(image);
      image.close();
      image = newImage;
      // Mirror arm/leg from top half to bottom half (same as vanilla)
      image.fillRect(0, 32, 64, 32, 0);
      image.copyRect(4, 16, 16, 32, 4, 4, true, false);
      image.copyRect(8, 16, 16, 32, 4, 4, true, false);
      image.copyRect(0, 20, 24, 32, 4, 12, true, false);
      image.copyRect(4, 20, 16, 32, 4, 12, true, false);
      image.copyRect(8, 20, 8, 32, 4, 12, true, false);
      image.copyRect(12, 20, 16, 32, 4, 12, true, false);
      image.copyRect(44, 16, -8, 32, 4, 4, true, false);
      image.copyRect(48, 16, -8, 32, 4, 4, true, false);
      image.copyRect(40, 20, 0, 32, 4, 12, true, false);
      image.copyRect(44, 20, -8, 32, 4, 12, true, false);
      image.copyRect(48, 20, -16, 32, 4, 12, true, false);
      image.copyRect(52, 20, -8, 32, 4, 12, true, false);
    }
    return image;
  }

  public static void invalidateChunk(String worldName, int chunkX, int chunkZ) {
    chunkProfiles.remove(chunkKey(worldName, chunkX, chunkZ));
  }

  public static void clear() {
    chunkProfiles.clear();
  }

  private static String chunkKey(String worldName, int chunkX, int chunkZ) {
    return worldName + ":" + chunkX + "," + chunkZ;
  }
}
