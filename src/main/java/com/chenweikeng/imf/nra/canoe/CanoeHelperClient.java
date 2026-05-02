package com.chenweikeng.imf.nra.canoe;

import com.chenweikeng.imf.ImfClient;
import com.chenweikeng.imf.nra.ServerState;
import com.chenweikeng.imf.nra.ride.CurrentRideHolder;
import com.chenweikeng.imf.nra.ride.RideName;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Owns the canoe logger lifecycle (CSV per session under {@code <gameDir>/canoe-logs/}) and exposes
 * signals other features need:
 *
 * <ul>
 *   <li>{@link #hasCanoeStarted()} — used by {@code CursorManager} to defer window minimisation
 *       until the player has actually started the canoe (first speed-bar update).
 *   <li>The session log — captures TICK / AB / CLICK rows for offline analysis.
 * </ul>
 *
 * <p>All accesses are routed through {@link #get()}; the mixin classes call {@link #onClick} and
 * {@link #onActionBar} from their respective hooks.
 */
public final class CanoeHelperClient {

  /** The display name of the paddle item. Used as the activation gate. */
  public static final String PADDLE_NAME = "Canoe Paddle";

  /** How long the paddle must be missing before the log file is closed. */
  private static final long IDLE_CLOSE_MS = 5_000;

  private static final CanoeHelperClient INSTANCE = new CanoeHelperClient();

  /** Last parsed action-bar state (so click rows can include the current speed snapshot). */
  private volatile CanoeBarParser.Parsed lastBar = new CanoeBarParser.Parsed("", Float.NaN, -1, -1);

  /** Wall-clock millis at which the most recent canoe-bar AB landed. */
  private volatile long lastCanoeBarMs = 0;

  /**
   * True once any canoe-bar AB has been received in the current paddle session. Cleared whenever
   * the helper closes a session (paddle gone for {@link #IDLE_CLOSE_MS}). This is the "the canoe
   * actually started moving" signal — used by {@code CursorManager} to defer window minimisation
   * until the player has had a chance to make the start click.
   */
  private volatile boolean canoeStarted = false;

  private CanoeLogger logger;

  /** Wall-clock millis at which we last saw the player holding the paddle. */
  private long lastPaddleSeenMs = 0;

  /** True while a session log file is open. */
  private boolean active = false;

  private CanoeHelperClient() {}

  public static CanoeHelperClient get() {
    return INSTANCE;
  }

  /** Wire client-tick callback. Call once from {@link com.chenweikeng.imf.ImfClient}. */
  public static void init() {
    ClientTickEvents.END_CLIENT_TICK.register(INSTANCE::onClientTick);
    ImfClient.LOGGER.info("[Canoe] helper initialised");
  }

  /**
   * Has the canoe started moving in the current session? Becomes true on the first canoe-bar
   * action-bar update and stays true until the paddle session ends (paddle gone for {@link
   * #IDLE_CLOSE_MS}). Used by {@code CursorManager} to gate window minimisation.
   */
  public boolean hasCanoeStarted() {
    return canoeStarted;
  }

  /* -------- intercepted from mixins -------- */

  /** Called from {@link com.chenweikeng.imf.mixin.CanoeGuiSetOverlayMessageMixin}. */
  public void onActionBar(Component component) {
    CanoeBarParser.Parsed parsed = CanoeBarParser.parse(component);
    lastBar = parsed;
    if (parsed.isCanoeBar()) {
      lastCanoeBarMs = System.currentTimeMillis();
      canoeStarted = true;
    }
    if (active && logger != null && parsed.isCanoeBar()) {
      writeRow(CanoeLogger.EventType.AB, parsed);
    }
  }

  /**
   * Called from {@link com.chenweikeng.imf.mixin.CanoeMinecraftClickMixin}. {@code right} is true
   * for {@code startUseItem}, false for {@code startAttack}.
   */
  public void onClick(boolean right) {
    LocalPlayer player = Minecraft.getInstance().player;
    if (player == null || !isHoldingPaddle(player)) return;
    if (active && logger != null) {
      writeRow(right ? CanoeLogger.EventType.CLICK_R : CanoeLogger.EventType.CLICK_L, lastBar);
    }
  }

  /* -------- per-tick driver -------- */

  private void onClientTick(Minecraft client) {
    if (!ServerState.isImagineFunServer()) {
      closeIfActive();
      return;
    }
    LocalPlayer player = client.player;
    if (player == null) {
      closeIfActive();
      return;
    }

    long now = System.currentTimeMillis();
    boolean holding = isHoldingPaddle(player);

    if (holding) {
      lastPaddleSeenMs = now;
      if (!active) {
        startSession();
      }
    } else if (active && now - lastPaddleSeenMs > IDLE_CLOSE_MS) {
      closeIfActive();
    }

    if (active && logger != null) {
      writeRow(CanoeLogger.EventType.TICK, lastBar);
    }

    publishProgress(player);
  }

  /**
   * If the player is currently on the canoe ride and the canoe has started, project their position
   * onto the reference track and publish the progress percent to {@link CurrentRideHolder}. Other
   * surfaces (ride plan HUD, strategy hub, macOS status bar) read from there.
   *
   * <p>If the canoe hasn't started yet, or the projection is too far from the reference path
   * (player is off-track somehow), we leave the progress at null rather than publishing noise.
   */
  private void publishProgress(LocalPlayer player) {
    if (CurrentRideHolder.getCurrentRide() != RideName.DAVY_CROCKETTS_EXPLORER_CANOES) return;
    if (!canoeStarted) return;
    Entity v = player.getVehicle();
    double x = v != null ? v.getX() : player.getX();
    double z = v != null ? v.getZ() : player.getZ();
    CanoeTrackModel track = CanoeTrackModel.get();
    if (!track.isLoaded()) return;
    if (track.distanceToTrack(x, z) > OFF_TRACK_THRESHOLD_BLOCKS) return;
    int p = track.progressPercent(x, z);
    if (p < 0) return;
    CurrentRideHolder.setCurrentProgressPercent(p);
  }

  /** If the boat strays farther than this from the reference path, drop the progress reading. */
  private static final float OFF_TRACK_THRESHOLD_BLOCKS = 15f;

  private void startSession() {
    CanoeLogger l = CanoeLogger.open();
    if (l == null) return;
    logger = l;
    active = true;
    lastBar = new CanoeBarParser.Parsed("", Float.NaN, -1, -1);
  }

  private void closeIfActive() {
    if (active && logger != null) {
      logger.close();
    }
    logger = null;
    active = false;
    canoeStarted = false;
    lastCanoeBarMs = 0;
  }

  /* -------- helpers -------- */

  private static boolean isHoldingPaddle(LocalPlayer player) {
    return isPaddle(player.getMainHandItem()) || isPaddle(player.getOffhandItem());
  }

  private static boolean isPaddle(ItemStack stack) {
    if (stack == null || stack.isEmpty()) return false;
    String name = stack.getHoverName().getString();
    return PADDLE_NAME.equals(name);
  }

  private void writeRow(CanoeLogger.EventType type, CanoeBarParser.Parsed bar) {
    Minecraft mc = Minecraft.getInstance();
    LocalPlayer player = mc.player;
    if (player == null || logger == null) return;
    Entity v = player.getVehicle();
    int vehicleId = v == null ? -1 : v.getId();
    Double vx = v == null ? null : v.getX();
    Double vy = v == null ? null : v.getY();
    Double vz = v == null ? null : v.getZ();
    Float vyaw = v == null ? null : v.getYRot();
    logger.write(
        type,
        player.getX(),
        player.getY(),
        player.getZ(),
        player.getYRot(),
        vehicleId,
        vx,
        vy,
        vz,
        vyaw,
        bar.speed,
        bar.fill,
        bar.total,
        bar.raw);
  }
}
