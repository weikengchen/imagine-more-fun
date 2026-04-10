package com.chenweikeng.imf.nra.handler;

import com.chenweikeng.imf.nra.Timing;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class WindowMinimizeHandler {
  private static final WindowMinimizeHandler INSTANCE = new WindowMinimizeHandler();

  private WindowMinimizeHandler() {}

  public static WindowMinimizeHandler getInstance() {
    return INSTANCE;
  }

  public void minimizeWindow() {
    Minecraft client = Minecraft.getInstance();
    if (client.getWindow() == null) {
      return;
    }

    long handle = client.getWindow().handle();
    boolean isMinimized = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE;

    if (!isMinimized) {
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
      client.execute(
          () -> {
            GLFW.glfwRestoreWindow(handle);
            GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_FLOATING, GLFW.GLFW_TRUE);

            client.execute(
                () -> {
                  long window = client.getWindow().handle();
                  GLFW.glfwShowWindow(window);

                  client.execute(
                      () -> {
                        long h = client.getWindow().handle();
                        GLFW.glfwFocusWindow(h);
                        GLFW.glfwRequestWindowAttention(h);
                        GLFW.glfwSetWindowAttrib(h, GLFW.GLFW_FLOATING, GLFW.GLFW_FALSE);

                        ReminderHandler.getInstance().lastAudioReminderTick =
                            -Timing.REMINDER_INTERVAL_TICKS;
                      });
                });
          });
    }
  }
}
