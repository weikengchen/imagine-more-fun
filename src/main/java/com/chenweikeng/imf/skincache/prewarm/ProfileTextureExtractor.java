package com.chenweikeng.imf.skincache.prewarm;

import com.google.common.collect.Iterables;
import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.item.component.ResolvableProfile;

/**
 * Extracts texture URL and hash directly from a ResolvableProfile's embedded properties, without
 * any network call.
 *
 * <p>The "textures" property in GameProfile contains a Base64-encoded JSON payload with the skin
 * URL. This is available immediately from NBT/network data, even for fake/random UUIDs.
 */
public final class ProfileTextureExtractor {

  private static final ConcurrentHashMap<String, SkinInfo> cache = new ConcurrentHashMap<>();

  private ProfileTextureExtractor() {}

  /**
   * Extract skin texture info from a ResolvableProfile. Returns null if the profile has no embedded
   * texture data. Results are cached by texture URL for fast repeat lookups.
   */
  public static SkinInfo extract(ResolvableProfile profile) {
    GameProfile gameProfile = profile.partialProfile();

    Property texturesProp = Iterables.getFirst(gameProfile.properties().get("textures"), null);
    if (texturesProp == null) return null;

    // Use the base64 value as cache key (same payload = same skin)
    String b64 = texturesProp.value();
    SkinInfo cached = cache.get(b64);
    if (cached != null) return cached;

    try {
      String json = new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();
      JsonObject textures = root.getAsJsonObject("textures");
      if (textures == null) return null;

      JsonObject skin = textures.getAsJsonObject("SKIN");
      if (skin == null) return null;

      String url = skin.get("url").getAsString();
      // Hash is the last path segment of the URL
      String hash = url.substring(url.lastIndexOf('/') + 1);
      // Replicate SkinManager.TextureCache ID generation: "skins/" + sha1(hash)
      String textureIdPath = "skins/" + Hashing.sha1().hashUnencodedChars(hash).toString();

      SkinInfo info = new SkinInfo(url, hash, textureIdPath);
      cache.put(b64, info);
      return info;
    } catch (Exception e) {
      return null;
    }
  }

  public record SkinInfo(String textureUrl, String textureHash, String textureIdPath) {}
}
