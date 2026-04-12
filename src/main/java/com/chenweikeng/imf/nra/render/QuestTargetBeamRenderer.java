package com.chenweikeng.imf.nra.render;

import com.chenweikeng.imf.nra.ServerState;
import com.chenweikeng.imf.nra.tracker.QuestTriangulationTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Renders a beacon beam at the estimated quest target location. Uses standard Minecraft rendering
 * with BeaconRenderer's beam texture.
 */
public final class QuestTargetBeamRenderer {

  // Beam color (golden/yellow for quest targets)
  private static final int BEAM_COLOR_R = 255;
  private static final int BEAM_COLOR_G = 215;
  private static final int BEAM_COLOR_B = 0;

  private QuestTargetBeamRenderer() {}

  /** Register the world render event. */
  public static void register() {
    WorldRenderEvents.AFTER_ENTITIES.register(
        context -> {
          if (!ServerState.isImagineFunServer()) {
            return;
          }
          render(context);
        });
  }

  /** Render the beacon beam at the estimated quest target. */
  public static void render(WorldRenderContext context) {
    QuestTriangulationTracker tracker = QuestTriangulationTracker.getInstance();
    Vec3 target = tracker.getEstimatedTarget();

    if (target == null || !tracker.hasConfidentEstimate()) {
      return;
    }

    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null || mc.player == null) {
      return;
    }

    Camera camera = mc.gameRenderer.getMainCamera();
    Vec3 cam = camera.position();

    // Don't render if too far away
    double distSq =
        (target.x - cam.x) * (target.x - cam.x) + (target.z - cam.z) * (target.z - cam.z);
    if (distSq > 256 * 256) { // 256 block render distance
      return;
    }

    PoseStack poseStack = context.matrices();
    BufferSource bufferSource = mc.renderBuffers().bufferSource();

    float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
    long gameTime = mc.level.getGameTime();

    poseStack.pushPose();

    // Translate to target position relative to camera
    poseStack.translate(target.x - cam.x, -cam.y, target.z - cam.z);

    // Center on block
    poseStack.translate(0.5, 0, 0.5);

    // Render the beam
    renderBeaconBeam(
        poseStack,
        bufferSource,
        partialTick,
        gameTime,
        0, // minY (start from ground)
        320, // maxY (to build limit)
        BEAM_COLOR_R,
        BEAM_COLOR_G,
        BEAM_COLOR_B);

    poseStack.popPose();

    // Flush the buffer
    bufferSource.endBatch();
  }

  /**
   * Render a beacon beam. Based on BeaconRenderer.renderBeaconBeam but simplified.
   *
   * @param poseStack The pose stack positioned at beam center
   * @param bufferSource The buffer source
   * @param partialTick Partial tick for animation
   * @param gameTime Game time for animation
   * @param minY Minimum Y (relative to pose stack)
   * @param maxY Maximum Y (relative to pose stack)
   * @param red Red component (0-255)
   * @param green Green component (0-255)
   * @param blue Blue component (0-255)
   */
  private static void renderBeaconBeam(
      PoseStack poseStack,
      BufferSource bufferSource,
      float partialTick,
      long gameTime,
      int minY,
      int maxY,
      int red,
      int green,
      int blue) {

    int height = maxY - minY;
    float animationTime = Math.floorMod(gameTime, 40L) + partialTick;

    // Texture scrolling
    float scroll = -animationTime;
    float texVOffset = Mth.frac(scroll * 0.2f - Mth.floor(scroll * 0.1f));

    // Inner beam (opaque, rotating)
    float innerRadius = 0.2f;
    int innerAlpha = 255;
    int innerColor = ARGB.color(innerAlpha, red, green, blue);

    poseStack.pushPose();
    poseStack.mulPose(Axis.YP.rotationDegrees(animationTime * 2.25f - 45.0f));

    renderBeamLayer(
        poseStack,
        bufferSource.getBuffer(RenderTypes.beaconBeam(BeaconRenderer.BEAM_LOCATION, false)),
        innerRadius,
        minY,
        height,
        texVOffset,
        innerColor);

    poseStack.popPose();

    // Outer beam (translucent, no rotation)
    float outerRadius = 0.25f;
    int outerAlpha = 32;
    int outerColor = ARGB.color(outerAlpha, red, green, blue);

    renderBeamLayer(
        poseStack,
        bufferSource.getBuffer(RenderTypes.beaconBeam(BeaconRenderer.BEAM_LOCATION, true)),
        outerRadius,
        minY,
        height,
        texVOffset,
        outerColor);
  }

  /**
   * Render a single layer of the beam (either inner or outer).
   *
   * @param poseStack The pose stack
   * @param buffer The vertex consumer
   * @param radius Beam radius
   * @param minY Minimum Y
   * @param height Beam height
   * @param texVOffset Texture V offset
   * @param color ARGB color
   */
  private static void renderBeamLayer(
      PoseStack poseStack,
      VertexConsumer buffer,
      float radius,
      int minY,
      int height,
      float texVOffset,
      int color) {

    PoseStack.Pose pose = poseStack.last();

    float texVMin = texVOffset;
    float texVMax = texVOffset + height;

    int light = 15728880; // Full brightness

    // Four faces of the beam
    // +X face
    addQuad(
        buffer,
        pose,
        radius,
        -radius,
        minY,
        radius,
        radius,
        minY + height,
        0,
        1,
        texVMin,
        texVMax,
        color,
        light);
    // -X face
    addQuad(
        buffer,
        pose,
        -radius,
        radius,
        minY,
        -radius,
        -radius,
        minY + height,
        0,
        1,
        texVMin,
        texVMax,
        color,
        light);
    // +Z face
    addQuad(
        buffer,
        pose,
        radius,
        radius,
        minY,
        -radius,
        radius,
        minY + height,
        0,
        1,
        texVMin,
        texVMax,
        color,
        light);
    // -Z face
    addQuad(
        buffer,
        pose,
        -radius,
        -radius,
        minY,
        radius,
        -radius,
        minY + height,
        0,
        1,
        texVMin,
        texVMax,
        color,
        light);
  }

  /**
   * Add a single quad to the buffer.
   *
   * @param buffer The vertex consumer
   * @param pose The pose
   * @param x1 First corner X
   * @param z1 First corner Z
   * @param y1 Bottom Y
   * @param x2 Second corner X
   * @param z2 Second corner Z
   * @param y2 Top Y
   * @param u1 Texture U start
   * @param u2 Texture U end
   * @param v1 Texture V start
   * @param v2 Texture V end
   * @param color ARGB color
   * @param light Light level
   */
  private static void addQuad(
      VertexConsumer buffer,
      PoseStack.Pose pose,
      float x1,
      float z1,
      float y1,
      float x2,
      float z2,
      float y2,
      float u1,
      float u2,
      float v1,
      float v2,
      int color,
      int light) {

    // Bottom-left
    buffer
        .addVertex(pose, x1, y1, z1)
        .setColor(color)
        .setUv(u1, v1)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(light)
        .setNormal(pose, 0, 1, 0);

    // Bottom-right
    buffer
        .addVertex(pose, x2, y1, z2)
        .setColor(color)
        .setUv(u2, v1)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(light)
        .setNormal(pose, 0, 1, 0);

    // Top-right
    buffer
        .addVertex(pose, x2, y2, z2)
        .setColor(color)
        .setUv(u2, v2)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(light)
        .setNormal(pose, 0, 1, 0);

    // Top-left
    buffer
        .addVertex(pose, x1, y2, z1)
        .setColor(color)
        .setUv(u1, v2)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(light)
        .setNormal(pose, 0, 1, 0);
  }
}
