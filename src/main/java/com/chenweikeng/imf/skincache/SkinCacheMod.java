package com.chenweikeng.imf.skincache;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chenweikeng.imf.skincache.cache.TextureCache;
import com.chenweikeng.imf.skincache.prewarm.ProfileCache;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SkinCacheMod implements ClientModInitializer {

    public static final String MOD_ID = "skincache";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static PrintWriter fileLog;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @Override
    public void onInitializeClient() {
        TextureCache.init();
        ProfileCache.init();
        initFileLog();
        log("SkinCache initialised — cache dir: " + TextureCache.getCacheDir());
        log("ProfileCache entries: " + ProfileCache.size());
        SkinCacheStats.start();
    }

    private static void initFileLog() {
        try {
            Path logDir = FabricLoader.getInstance().getGameDir().resolve("skincache");
            Files.createDirectories(logDir);
            Path logFile = logDir.resolve("skincache.log");
            fileLog = new PrintWriter(new FileWriter(logFile.toFile(), false), true);
            fileLog.println("=== SkinCache log started " + LocalDateTime.now() + " ===");
        } catch (IOException e) {
            LOGGER.error("[SkinCache] Failed to create log file", e);
        }
    }

    /**
     * Write to .minecraft/skincache/skincache.log — separate from game log.
     */
    public static void log(String msg) {
        if (fileLog != null) {
            String ts = LocalDateTime.now().format(TIME_FMT);
            String thread = Thread.currentThread().getName();
            fileLog.println("[" + ts + "] [" + thread + "] " + msg);
        }
    }
}
