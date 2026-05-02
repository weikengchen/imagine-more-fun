package com.chenweikeng.imf.nra.spacemountain;

import com.chenweikeng.imf.nra.NotRidingAlertClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Custom starfield renderer for Space and Hyperspace Mountain. Draws a fixed cloud of textured
 * billboard quads each frame via {@link WorldRenderEvents#AFTER_ENTITIES}. Chosen over the particle
 * system because particles get aggressively distance-culled — at ride speed the player would see
 * stars pop in/out as they move, which breaks immersion.
 *
 * <p><b>Render type.</b> {@link RenderTypes#eyes} uses {@link
 * net.minecraft.client.renderer.RenderPipelines#EYES} which is {@code EMISSIVE} (lightmap bypassed,
 * always full-bright), translucent-blended, depth-test on, depth-write off. So:
 *
 * <ul>
 *   <li>Stars stay bright in pitch-black show-building interiors.
 *   <li>Walls in front of a star occlude it (depth test); stars don't z-fight each other (no depth
 *       write).
 *   <li>Quads are not light sources: the {@code FULL_BRIGHT} lightmap UV we set is consumed by the
 *       star's own fragment shader, never propagated to neighboring blocks.
 * </ul>
 *
 * <p><b>Anchor.</b> The cloud is generated on the first frame the override goes active and held
 * until it goes inactive — so star positions are fixed in world space for the duration of the ride,
 * and the player flies through them. Each ride re-anchors to the player's then-current position
 * with the same RNG seed, so the relative pattern is identical session-to-session.
 */
public final class SpaceMountainStarRenderer {
  private static final Identifier STAR_TEXTURE =
      Identifier.fromNamespaceAndPath("imaginemorefun", "textures/particle/star.png");

  private static final int STAR_COUNT = 1500;
  private static final double SPAWN_RADIUS = 60.0;
  private static final float STAR_SIZE_MIN = 0.18f;
  private static final float STAR_SIZE_MAX = 0.55f;
  private static final long SEED = 0xCAFEBABEL;

  // Dome center for both Space Mountain and Hyperspace Mountain (same physical building on the
  // ImagineFun server, retheme overlay seasonally). Identified by flood-fill analysis of a
  // chunk dump captured mid-ride: dome interior at this Y-level (player elevation 85) is a
  // ~80x80 hollow centered on (-270, 80, 167) with floor at Y≈62 and ceiling around Y≈100.
  private static final double DOME_CENTER_X = -270.0;
  private static final double DOME_CENTER_Y = 80.0;
  private static final double DOME_CENTER_Z = 167.0;

  private static double[] starX = new double[0];
  private static double[] starY = new double[0];
  private static double[] starZ = new double[0];
  private static float[] starHalfSize = new float[0];
  private static boolean haveAnchor = false;

  private SpaceMountainStarRenderer() {}

  public static void register() {
    WorldRenderEvents.AFTER_ENTITIES.register(SpaceMountainStarRenderer::render);
  }

  private static void render(WorldRenderContext ctx) {
    if (!SpaceMountainOverride.isActive()) {
      if (haveAnchor) {
        haveAnchor = false;
        NotRidingAlertClient.LOGGER.info("[SpaceMountainStarRenderer] cleared anchor");
      }
      return;
    }
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.level == null) return;

    if (!haveAnchor) {
      // Anchor at the dome center, NOT the boarding point. Same coord works for both rides
      // because Space Mountain and Hyperspace Mountain share one physical building.
      generateAt(DOME_CENTER_X, DOME_CENTER_Y, DOME_CENTER_Z);
    }
    drawStars(ctx, mc);
  }

  private static void generateAt(double cx, double cy, double cz) {
    Random rng = new Random(SEED);
    starX = new double[STAR_COUNT];
    starY = new double[STAR_COUNT];
    starZ = new double[STAR_COUNT];
    starHalfSize = new float[STAR_COUNT];
    for (int i = 0; i < STAR_COUNT; i++) {
      // Uniform-in-volume sphere sample: cube-root the radial random.
      double r = SPAWN_RADIUS * Math.cbrt(rng.nextDouble());
      double theta = rng.nextDouble() * 2 * Math.PI;
      double phi = Math.acos(2 * rng.nextDouble() - 1);
      double sinPhi = Math.sin(phi);
      starX[i] = cx + r * sinPhi * Math.cos(theta);
      starY[i] = cy + r * Math.cos(phi);
      starZ[i] = cz + r * sinPhi * Math.sin(theta);
      float scale = STAR_SIZE_MIN + (STAR_SIZE_MAX - STAR_SIZE_MIN) * rng.nextFloat();
      starHalfSize[i] = scale * 0.5f;
    }
    haveAnchor = true;
    NotRidingAlertClient.LOGGER.info(
        "[SpaceMountainStarRenderer] generated {} stars centered at ({}, {}, {})",
        STAR_COUNT,
        cx,
        cy,
        cz);
  }

  private static void drawStars(WorldRenderContext ctx, Minecraft mc) {
    Camera camera = mc.gameRenderer.getMainCamera();
    Vec3 cam = camera.position();

    // Camera-local right/up axes mapped into world space. Particles do this via push/popPose
    // per particle, but since all our quads share the same orientation we can compute these
    // axes once and use them as offsets.
    Quaternionf rot = camera.rotation();
    Vector3f right = rot.transform(new Vector3f(1f, 0f, 0f));
    Vector3f up = rot.transform(new Vector3f(0f, 1f, 0f));

    PoseStack poseStack = ctx.matrices();
    PoseStack.Pose pose = poseStack.last();
    BufferSource bufferSource = mc.renderBuffers().bufferSource();
    RenderType renderType = RenderTypes.eyes(STAR_TEXTURE);
    VertexConsumer vc = bufferSource.getBuffer(renderType);

    int light = LightTexture.FULL_BRIGHT;
    int overlay = OverlayTexture.NO_OVERLAY;
    float camX = (float) cam.x;
    float camY = (float) cam.y;
    float camZ = (float) cam.z;

    for (int i = 0; i < starX.length; i++) {
      float h = starHalfSize[i];
      float wx = (float) (starX[i] - camX);
      float wy = (float) (starY[i] - camY);
      float wz = (float) (starZ[i] - camZ);

      float rx = right.x * h;
      float ry = right.y * h;
      float rz = right.z * h;
      float ux = up.x * h;
      float uy = up.y * h;
      float uz = up.z * h;

      // CCW from upper-left: (-r,+u), (-r,-u), (+r,-u), (+r,+u)
      addVertex(vc, pose, wx - rx + ux, wy - ry + uy, wz - rz + uz, 0f, 0f, light, overlay);
      addVertex(vc, pose, wx - rx - ux, wy - ry - uy, wz - rz - uz, 0f, 1f, light, overlay);
      addVertex(vc, pose, wx + rx - ux, wy + ry - uy, wz + rz - uz, 1f, 1f, light, overlay);
      addVertex(vc, pose, wx + rx + ux, wy + ry + uy, wz + rz + uz, 1f, 0f, light, overlay);
    }
    bufferSource.endBatch(renderType);
  }

  private static void addVertex(
      VertexConsumer vc,
      PoseStack.Pose pose,
      float x,
      float y,
      float z,
      float u,
      float v,
      int light,
      int overlay) {
    vc.addVertex(pose, x, y, z)
        .setColor(1f, 1f, 1f, 1f)
        .setUv(u, v)
        .setOverlay(overlay)
        .setLight(light)
        .setNormal(pose, 0f, 1f, 0f);
  }
}
