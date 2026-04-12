package com.chenweikeng.imf.skincache;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Aggregates render-path events into counters and periodically flushes a single summary line to
 * {@code skincache.log}. Replaces per-hash spam from the mixins.
 *
 * <p>Counters are incremented from any thread; the flush thread runs once every {@link
 * #FLUSH_INTERVAL_SEC} and only emits a line when something changed.
 */
public final class SkinCacheStats {

  /**
   * Compile-time toggle. Flip to {@code true} and rebuild to re-enable per-hash spam in {@code
   * skincache.log} for debugging — emits one SHORT-CIRCUIT line per unique texture hash and one
   * MISS line per unique reason+key pair (deduped, same as the original behavior).
   *
   * <p>Because this is a {@code static final boolean}, the JIT and javac dead-code-eliminate every
   * {@code if (DEBUG_PER_HASH)} branch when it's {@code false}, so leaving it off has zero runtime
   * cost.
   */
  public static final boolean DEBUG_PER_HASH = false;

  private static final long FLUSH_INTERVAL_SEC = 30;

  // Render-path mixin counters
  public static final AtomicLong customHeadShortCircuit = new AtomicLong();
  public static final AtomicLong customHeadMissNoPng = new AtomicLong();
  public static final AtomicLong customHeadMissOther = new AtomicLong();
  public static final AtomicLong skullShortCircuit = new AtomicLong();
  public static final AtomicLong skullMissNoPng = new AtomicLong();
  public static final AtomicLong skullMissOther = new AtomicLong();

  // TextureRegistrar counters
  public static final AtomicLong texturesRegistered = new AtomicLong();
  public static final AtomicLong textureRegisterFailed = new AtomicLong();

  // Snapshot of last flushed values, so we only emit when something changed
  private static long lastCustomHeadSC, lastCustomHeadMissNoPng, lastCustomHeadMissOther;
  private static long lastSkullSC, lastSkullMissNoPng, lastSkullMissOther;
  private static long lastTexReg, lastTexRegFail;

  private static ScheduledExecutorService flusher;

  private SkinCacheStats() {}

  public static void start() {
    if (flusher != null) return;
    flusher =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "skincache-stats");
              t.setDaemon(true);
              return t;
            });
    flusher.scheduleAtFixedRate(
        SkinCacheStats::flush, FLUSH_INTERVAL_SEC, FLUSH_INTERVAL_SEC, TimeUnit.SECONDS);
  }

  private static void flush() {
    long chSC = customHeadShortCircuit.get();
    long chMNP = customHeadMissNoPng.get();
    long chMO = customHeadMissOther.get();
    long skSC = skullShortCircuit.get();
    long skMNP = skullMissNoPng.get();
    long skMO = skullMissOther.get();
    long tReg = texturesRegistered.get();
    long tFail = textureRegisterFailed.get();

    boolean changed =
        chSC != lastCustomHeadSC
            || chMNP != lastCustomHeadMissNoPng
            || chMO != lastCustomHeadMissOther
            || skSC != lastSkullSC
            || skMNP != lastSkullMissNoPng
            || skMO != lastSkullMissOther
            || tReg != lastTexReg
            || tFail != lastTexRegFail;

    if (!changed) return;

    long dCH_SC = chSC - lastCustomHeadSC;
    long dCH_NP = chMNP - lastCustomHeadMissNoPng;
    long dCH_MO = chMO - lastCustomHeadMissOther;
    long dSK_SC = skSC - lastSkullSC;
    long dSK_NP = skMNP - lastSkullMissNoPng;
    long dSK_MO = skMO - lastSkullMissOther;
    long dTReg = tReg - lastTexReg;
    long dTFail = tFail - lastTexRegFail;

    SkinCacheMod.log(
        String.format(
            "[stats] head: +%d sc / +%d miss(noPng) / +%d miss(other)  |  "
                + "skull: +%d sc / +%d miss(noPng) / +%d miss(other)  |  "
                + "tex: +%d reg / +%d failed  ||  totals  head:%d/%d/%d  skull:%d/%d/%d  tex:%d/%d",
            dCH_SC, dCH_NP, dCH_MO, dSK_SC, dSK_NP, dSK_MO, dTReg, dTFail, chSC, chMNP, chMO, skSC,
            skMNP, skMO, tReg, tFail));

    lastCustomHeadSC = chSC;
    lastCustomHeadMissNoPng = chMNP;
    lastCustomHeadMissOther = chMO;
    lastSkullSC = skSC;
    lastSkullMissNoPng = skMNP;
    lastSkullMissOther = skMO;
    lastTexReg = tReg;
    lastTexRegFail = tFail;
  }
}
