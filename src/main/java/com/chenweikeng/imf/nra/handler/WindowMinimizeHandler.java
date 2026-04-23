package com.chenweikeng.imf.nra.handler;

import com.chenweikeng.imf.nra.Timing;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WindowMinimizeHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger("imaginemorefun/window");
  private static final WindowMinimizeHandler INSTANCE = new WindowMinimizeHandler();
  // Monitor for 15 seconds (300 ticks @ 20tps) after each restore; long enough
  // to catch a click-induced resize/iconify but short enough to avoid log spam.
  private static final int MONITOR_TICKS = 300;
  // If GLFW hands us a frame this small in either axis during the post-restore
  // grace window, assume something (deminiaturize animation frame, window-snap
  // to narrow strip, etc.) bogusly shrank us and force the pre-minimize
  // geometry back. 500 comfortably covers both the ~73x175 dock-thumbnail case
  // and the ~208x1022 narrow-strip snap we've seen in practice.
  private static final int SHRINK_THRESHOLD_PX = 500;

  private int monitorTicksLeft = 0;
  private int lastW = -1;
  private int lastH = -1;
  private int lastX = Integer.MIN_VALUE;
  private int lastY = Integer.MIN_VALUE;
  private boolean lastIconified = false;
  private boolean lastMaximized = false;

  private int savedW = -1;
  private int savedH = -1;
  private int savedX = Integer.MIN_VALUE;
  private int savedY = Integer.MIN_VALUE;
  private boolean savedMaximized = false;
  private boolean hasSavedState = false;
  private boolean recoveryAttempted = false;

  private WindowMinimizeHandler() {}

  public static WindowMinimizeHandler getInstance() {
    return INSTANCE;
  }

  private void logSnapshot(long handle, String tag) {
    int[] px = new int[1], py = new int[1];
    int[] sw = new int[1], sh = new int[1];
    double[] cx = new double[1], cy = new double[1];
    GLFW.glfwGetWindowPos(handle, px, py);
    GLFW.glfwGetWindowSize(handle, sw, sh);
    GLFW.glfwGetCursorPos(handle, cx, cy);
    boolean iconified = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE;
    boolean maximized = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE;
    LOGGER.info(
        "[{}] pos=({},{}) size={}x{} iconified={} maximized={} cursorWin=({},{}) cursorScreen=({},{})",
        tag,
        px[0],
        py[0],
        sw[0],
        sh[0],
        iconified,
        maximized,
        (int) cx[0],
        (int) cy[0],
        px[0] + (int) cx[0],
        py[0] + (int) cy[0]);
  }

  private void startMonitoring(long handle) {
    int[] sw = new int[1], sh = new int[1];
    int[] px = new int[1], py = new int[1];
    GLFW.glfwGetWindowSize(handle, sw, sh);
    GLFW.glfwGetWindowPos(handle, px, py);
    lastW = sw[0];
    lastH = sh[0];
    lastX = px[0];
    lastY = py[0];
    lastIconified = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE;
    lastMaximized = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE;
    monitorTicksLeft = MONITOR_TICKS;
    LOGGER.info("[monitor] start ({} ticks)", MONITOR_TICKS);
  }

  public void tickMonitor() {
    if (monitorTicksLeft <= 0) return;
    Minecraft client = Minecraft.getInstance();
    if (client.getWindow() == null) {
      monitorTicksLeft = 0;
      return;
    }
    long h = client.getWindow().handle();

    int[] sw = new int[1], sh = new int[1];
    int[] px = new int[1], py = new int[1];
    GLFW.glfwGetWindowSize(h, sw, sh);
    GLFW.glfwGetWindowPos(h, px, py);
    boolean ic = GLFW.glfwGetWindowAttrib(h, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE;
    boolean mx = GLFW.glfwGetWindowAttrib(h, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE;

    if (sw[0] != lastW || sh[0] != lastH) {
      double[] cx = new double[1], cy = new double[1];
      GLFW.glfwGetCursorPos(h, cx, cy);
      LOGGER.info(
          "[monitor] size {}x{} -> {}x{} cursorWin=({},{}) ticksLeft={}",
          lastW,
          lastH,
          sw[0],
          sh[0],
          (int) cx[0],
          (int) cy[0],
          monitorTicksLeft);
      lastW = sw[0];
      lastH = sh[0];
    }

    if (!recoveryAttempted
        && hasSavedState
        && (sw[0] < SHRINK_THRESHOLD_PX || sh[0] < SHRINK_THRESHOLD_PX)) {
      recoveryAttempted = true;
      LOGGER.warn(
          "[monitor] shrink detected {}x{} at ({},{}); restoring to {}x{} at ({},{}) maximized={}",
          sw[0],
          sh[0],
          px[0],
          py[0],
          savedW,
          savedH,
          savedX,
          savedY,
          savedMaximized);
      int targetW = savedW, targetH = savedH, targetX = savedX, targetY = savedY;
      boolean targetMaximized = savedMaximized;
      client.execute(
          () -> {
            GLFW.glfwSetWindowPos(h, targetX, targetY);
            GLFW.glfwSetWindowSize(h, targetW, targetH);
            if (targetMaximized) {
              GLFW.glfwMaximizeWindow(h);
            }
          });
    }
    if (px[0] != lastX || py[0] != lastY) {
      LOGGER.info(
          "[monitor] pos ({},{}) -> ({},{}) ticksLeft={}",
          lastX,
          lastY,
          px[0],
          py[0],
          monitorTicksLeft);
      lastX = px[0];
      lastY = py[0];
    }
    if (ic != lastIconified) {
      double[] cx = new double[1], cy = new double[1];
      GLFW.glfwGetCursorPos(h, cx, cy);
      LOGGER.info(
          "[monitor] iconified {} -> {} cursorWin=({},{}) ticksLeft={}",
          lastIconified,
          ic,
          (int) cx[0],
          (int) cy[0],
          monitorTicksLeft);
      lastIconified = ic;
    }
    if (mx != lastMaximized) {
      LOGGER.info("[monitor] maximized {} -> {} ticksLeft={}", lastMaximized, mx, monitorTicksLeft);
      lastMaximized = mx;
    }

    monitorTicksLeft--;
    if (monitorTicksLeft == 0) {
      LOGGER.info("[monitor] stop");
    }
  }

  public void minimizeWindow() {
    Minecraft client = Minecraft.getInstance();
    if (client.getWindow() == null) {
      return;
    }

    long handle = client.getWindow().handle();
    boolean isMinimized = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE;

    if (!isMinimized) {
      logSnapshot(handle, "before-minimize");
      int[] sw = new int[1], sh = new int[1];
      int[] px = new int[1], py = new int[1];
      GLFW.glfwGetWindowSize(handle, sw, sh);
      GLFW.glfwGetWindowPos(handle, px, py);
      savedW = sw[0];
      savedH = sh[0];
      savedX = px[0];
      savedY = py[0];
      savedMaximized = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE;
      hasSavedState = true;
      client.execute(
          () -> {
            GLFW.glfwIconifyWindow(handle);
          });
    }
  }

  public void requestAttention() {
    Minecraft client = Minecraft.getInstance();
    if (client.getWindow() == null) {
      return;
    }

    long handle = client.getWindow().handle();
    boolean isMinimized = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE;

    if (!isMinimized) {
      client.execute(
          () -> {
            long h = client.getWindow().handle();
            GLFW.glfwFocusWindow(h);
            GLFW.glfwRequestWindowAttention(h);
          });
    }
  }

  public void restoreWindow() {
    Minecraft client = Minecraft.getInstance();
    if (client.getWindow() == null) {
      return;
    }

    long handle = client.getWindow().handle();
    boolean isMinimized = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE;

    if (isMinimized) {
      logSnapshot(handle, "before-restore");
      client.execute(
          () -> {
            GLFW.glfwRestoreWindow(handle);
            GLFW.glfwFocusWindow(handle);
            GLFW.glfwRequestWindowAttention(handle);

            logSnapshot(handle, "after-restore");
            recoveryAttempted = false;
            startMonitoring(handle);

            ReminderHandler.getInstance().lastAudioReminderTick = -Timing.REMINDER_INTERVAL_TICKS;
          });
    }
  }
}
