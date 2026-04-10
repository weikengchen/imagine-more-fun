package com.chenweikeng.imf.nra.handler;

import com.chenweikeng.imf.nra.GameState;
import com.chenweikeng.imf.nra.NotRidingAlertClient;
import com.chenweikeng.imf.nra.config.ModConfig;
import com.chenweikeng.imf.nra.ride.AutograbHolder;
import com.chenweikeng.imf.nra.ride.AutograbHolder.Point;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

public class AutograbRegionRenderer {
  private static final double RENDER_DISTANCE = 50.0;

  public static void register() {
    WorldRenderEvents.AFTER_ENTITIES.register(
        context -> {
          if (!NotRidingAlertClient.isImagineFunServer()) {
            return;
          }
          render(context);
        });
  }

  public static void render(WorldRenderContext context) {
    if (!ModConfig.currentSetting.showAutograbRegions) {
      return;
    }

    if (GameState.getInstance().isRiding()) {
      return;
    }

    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.level == null) {
      return;
    }

    PoseStack poseStack = context.matrices();
    Camera camera = mc.gameRenderer.getMainCamera();
    Vec3 cam = camera.position();

    BufferSource bufferSource = mc.renderBuffers().bufferSource();

    for (AutograbHolder.AutograbRegion region : AutograbHolder.regions()) {
      if (!region.filter().test(mc)) {
        continue;
      }

      double dx = cam.x - region.center().x;
      double dz = cam.z - region.center().z;
      double distance = Math.sqrt(dx * dx + dz * dz);

      if (distance <= RENDER_DISTANCE) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.debugTriangleFan());
        drawRegion(buffer, poseStack, cam, region);
        bufferSource.endBatch(RenderTypes.debugTriangleFan());
      }
    }
  }

  private static void drawRegion(
      VertexConsumer buffer, PoseStack poseStack, Vec3 cam, AutograbHolder.AutograbRegion region) {
    Point[] points = region.points();
    double y = region.y();
    PoseStack.Pose pose = poseStack.last();

    if (points.length < 2) {
      return;
    }

    for (int i = 0; i < points.length; i++) {
      Point p1 = points[i];
      Point p2 = points[(i + 1) % points.length];

      buffer
          .addVertex(
              pose, (float) (p1.x - cam.x), (float) (y + 0.275 - cam.y), (float) (p1.z - cam.z))
          .setColor(0f, 0.8f, 0f, 0.3f)
          .setNormal(pose, 0.0f, 1.0f, 0.0f)
          .setLineWidth(1.0f);

      buffer
          .addVertex(
              pose, (float) (p2.x - cam.x), (float) (y + 0.275 - cam.y), (float) (p2.z - cam.z))
          .setColor(0f, 0.8f, 0f, 0.3f)
          .setNormal(pose, 0.0f, 1.0f, 0.0f)
          .setLineWidth(1.0f);
    }
  }
}
