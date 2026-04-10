package com.chenweikeng.imf.nra.report.ui;

import com.chenweikeng.imf.nra.NotRidingAlertClient;
import com.mojang.blaze3d.platform.NativeImage;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Handles copying a ride-report screenshot to the clipboard and opening Discord. */
public final class DiscordShareUtil {
  private static final String GUILD_ID = "410989228315639829";
  private static final String CHANNEL_ID = "451802998281338880";
  private static final String DISCORD_DEEP_LINK =
      "discord://discord.com/channels/" + GUILD_ID + "/" + CHANNEL_ID;

  private DiscordShareUtil() {}

  /**
   * Crop a region from a {@link NativeImage} and convert it to a {@link BufferedImage}.
   * NativeImage.getPixel() returns ARGB which maps directly to BufferedImage TYPE_INT_ARGB.
   */
  public static BufferedImage nativeImageToBufferedImage(
      NativeImage src, int x0, int y0, int cropW, int cropH) {
    int srcW = src.getWidth();
    int srcH = src.getHeight();
    int endX = Math.min(x0 + cropW, srcW);
    int endY = Math.min(y0 + cropH, srcH);
    int w = Math.max(endX - x0, 1);
    int h = Math.max(endY - y0, 1);

    BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    for (int py = 0; py < h; py++) {
      for (int px = 0; px < w; px++) {
        out.setRGB(px, py, src.getPixel(x0 + px, y0 + py));
      }
    }
    return out;
  }

  /**
   * Copy an image to the system clipboard using OS-native commands. Java writes the PNG to a temp
   * file; the OS clipboard tool reads it natively — no AWT Toolkit involved (AWT clipboard is
   * isolated from the system pasteboard in GLFW apps like Minecraft).
   *
   * @return true if clipboard was set successfully
   */
  public static boolean copyImageToClipboard(BufferedImage image) {
    Path tmp = null;
    try {
      tmp = Files.createTempFile("nra-report-", ".png");
      ImageIO.write(image, "png", tmp.toFile());

      String os = System.getProperty("os.name", "").toLowerCase();
      int exitCode;
      if (os.contains("mac") || os.contains("darwin")) {
        exitCode =
            new ProcessBuilder(
                    "osascript",
                    "-e",
                    "set the clipboard to (read (POSIX file \"" + tmp + "\") as «class PNGf»)")
                .redirectErrorStream(true)
                .start()
                .waitFor();
      } else if (os.contains("win")) {
        String ps =
            "Add-Type -AssemblyName System.Windows.Forms;"
                + "[System.Windows.Forms.Clipboard]::SetImage("
                + "[System.Drawing.Image]::FromFile('"
                + tmp
                + "'))";
        exitCode =
            new ProcessBuilder("powershell", "-command", ps)
                .redirectErrorStream(true)
                .start()
                .waitFor();
      } else {
        // Linux X11
        exitCode =
            new ProcessBuilder(
                    "xclip", "-selection", "clipboard", "-t", "image/png", "-i", tmp.toString())
                .redirectErrorStream(true)
                .start()
                .waitFor();
      }

      if (exitCode != 0) {
        NotRidingAlertClient.LOGGER.warn(
            "Native clipboard command returned exit code {}", exitCode);
        return false;
      }
      NotRidingAlertClient.LOGGER.info(
          "Copied {}x{} report image to system clipboard", image.getWidth(), image.getHeight());
      return true;
    } catch (Exception e) {
      NotRidingAlertClient.LOGGER.warn("Failed to copy image to system clipboard", e);
      return false;
    } finally {
      if (tmp != null) {
        try {
          Files.deleteIfExists(tmp);
        } catch (Exception ignored) {
        }
      }
    }
  }

  /** Open Discord to the ImagineFun ride-report channel via deep link. */
  public static void openDiscordChannel() {
    try {
      if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(new URI(DISCORD_DEEP_LINK));
      } else {
        openFallback();
      }
    } catch (Exception e) {
      NotRidingAlertClient.LOGGER.warn("Failed to open Discord deep link", e);
      try {
        openFallback();
      } catch (Exception ex) {
        NotRidingAlertClient.LOGGER.warn("Fallback Discord open also failed", ex);
      }
    }
  }

  private static void openFallback() throws Exception {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("mac")) {
      Runtime.getRuntime().exec(new String[] {"open", DISCORD_DEEP_LINK});
    } else if (os.contains("win")) {
      Runtime.getRuntime()
          .exec(new String[] {"rundll32", "url.dll,FileProtocolHandler", DISCORD_DEEP_LINK});
    } else {
      Runtime.getRuntime().exec(new String[] {"xdg-open", DISCORD_DEEP_LINK});
    }
  }
}
