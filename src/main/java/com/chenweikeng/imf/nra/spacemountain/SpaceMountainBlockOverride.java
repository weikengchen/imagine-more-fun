package com.chenweikeng.imf.nra.spacemountain;

import com.chenweikeng.imf.nra.NotRidingAlertClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client-side block-state replacement applied while the player is riding Space or Hyperspace
 * Mountain. The {@link #apply} method is invoked from {@code ClientLevel.getBlockState} via mixin
 * and may return a different {@code BlockState} for visual purposes — the server still holds
 * authoritative state, so collisions and pathfinding are unaffected.
 *
 * <h2>Current rules</h2>
 *
 * <ul>
 *   <li><b>PeopleMover hole cover.</b> A horizontal viewing window cut into the dome's south wall
 *       (originally so PeopleMover passengers can glimpse the show) breaks the immersion when seen
 *       from inside the ride. We cover it by replacing AIR with {@code black_concrete} inside a
 *       hand-picked bbox around the opening. The wall material here is black_concrete (734 adjacent
 *       blocks observed in a chunk dump), so the patch blends with the existing structure.
 * </ul>
 *
 * <p>Chunk meshes are cached per section, so changes to {@code getBlockState} return values don't
 * repaint the world automatically. {@link #init} watches the active flag every tick and forces a
 * full re-mesh on transitions. After the initial repaint, ordinary chunk updates pick up further
 * changes naturally.
 */
public final class SpaceMountainBlockOverride {
  // Hole bbox identified by flood-fill from the user-supplied coord (-253, 76, 216) on a chunk
  // dump captured during a real ride. See debug-dumps/parse_dump.py + the analysis run that
  // produced these bounds — the air region is ~1.9k cells.
  private static final int HOLE_X_MIN = -283;
  private static final int HOLE_X_MAX = -223;
  private static final int HOLE_Y_MIN = 74;
  private static final int HOLE_Y_MAX = 83;
  private static final int HOLE_Z_MIN = 215;
  private static final int HOLE_Z_MAX = 220;

  private static final BlockState COVER = Blocks.BLACK_CONCRETE.defaultBlockState();

  private static boolean previousActive = false;

  private SpaceMountainBlockOverride() {}

  public static void init() {
    ClientTickEvents.END_CLIENT_TICK.register(SpaceMountainBlockOverride::onTick);
  }

  /**
   * Called from the ClientLevel mixin on every block-state lookup. Hot path — keep the {@code
   * isActive()} early-return first so off-ride traffic pays at most one boolean check.
   */
  public static BlockState apply(BlockState original, BlockPos pos) {
    if (!SpaceMountainOverride.isActive()) return original;

    int x = pos.getX();
    int y = pos.getY();
    int z = pos.getZ();
    if (x >= HOLE_X_MIN
        && x <= HOLE_X_MAX
        && y >= HOLE_Y_MIN
        && y <= HOLE_Y_MAX
        && z >= HOLE_Z_MIN
        && z <= HOLE_Z_MAX
        && isVisuallyEmpty(original)) {
      return COVER;
    }
    return original;
  }

  /**
   * "Visually empty" — anything the user sees through. Barriers and Light blocks are technically
   * solid but render as nothing, so the south wall has a chain of barriers along Z=215 that the
   * naive {@code isAir()} rule lets straight through. We cover those too.
   */
  private static boolean isVisuallyEmpty(BlockState state) {
    return state.isAir() || state.is(Blocks.BARRIER) || state.is(Blocks.LIGHT);
  }

  private static void onTick(Minecraft mc) {
    boolean active = SpaceMountainOverride.isActive();
    if (active == previousActive) return;
    previousActive = active;
    if (mc.levelRenderer != null) {
      mc.levelRenderer.allChanged();
      NotRidingAlertClient.LOGGER.info(
          "[SpaceMountainBlockOverride] active={} → forced full re-mesh", active);
    }
  }
}
