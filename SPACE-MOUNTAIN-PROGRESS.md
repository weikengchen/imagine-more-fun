# Space Mountain / Hyperspace Mountain client overrides

Client-side modifications that turn Hyperspace Mountain (and Space Mountain — same building, seasonal overlay) into a proper dark "ride through space". Server is untouched; we only re-render what the client sees.

Everything is gated by `SpaceMountainOverride.isActive()` — true iff connected to ImagineFun **and** `CurrentRideHolder.getCurrentRide()` is `SPACE_MOUNTAIN` or `HYPERSPACE_MOUNTAIN`. Off the ride, every override fast-paths back to vanilla in a single boolean check.

## Deployed overrides

All in `com.chenweikeng.imf.mixin.*` and `com.chenweikeng.imf.nra.spacemountain.*`.

| Concern | Where | What it does |
|---|---|---|
| Master gate | `SpaceMountainOverride.isActive()` | Single check used by every override below |
| Black fog & sky | `NraEnvironmentAttributeProbeMixin` | Forces `FOG_COLOR` / `SKY_COLOR` / `SKY_LIGHT_COLOR` to `0` so distant objects fade to black instead of pale blue |
| Suppress server starfield | `NraParticleEngineMixin` | Cancels every `minecraft:end_rod` particle at `ParticleEngine.createParticle` (the server spams ~480/frame around riders) |
| Custom starfield | `SpaceMountainStarRenderer` (via `WorldRenderEvents.AFTER_ENTITIES`) | 1500 fixed-position billboards in a 60-block sphere at `(-270, 80, 167)`. Renders with `RenderTypes.eyes` (additive emissive, depth-test on, depth-write off). Bypasses the particle system to avoid distance culling |
| PeopleMover hole cover | `SpaceMountainBlockOverride` + `NraClientLevelGetBlockStateMixin` | Inside bbox `X:[-283,-223] Y:[74,83] Z:[215,220]`, replaces `air ∪ barrier ∪ light` with `black_concrete`. Forces chunk re-mesh on ride enter/exit |
| Show-armor-stand brightness | `NraEntityRenderDispatcherLightMixin` | All `ArmorStand` entities render at `LightTexture.FULL_BRIGHT` while on the ride, so TIE Fighter / X-Wing prop helmets stay vivid against the dark dome |

Mixin registrations: [imf.mixins.json](src/main/resources/imf.mixins.json).
Init wiring: [ImfClient.java](src/main/java/com/chenweikeng/imf/ImfClient.java).

## Key findings (from chunk-dump analysis)

- **Dimension is `minecraft:dlnew`** — one dim hosting many rides side by side on a 368×368 horizontal grid. The whole-world bbox is misleading; per-ride analysis needs a small window around the player.
- **Dome interior** at player elevation is a ~80×80 hollow centered on `(-270, 80, 167)`. Floor Y≈62, ceiling around Y≈100 with gaps above.
- **The "distant brightness" was fog, not sky-light leak.** Sky light at the player is `0`. The original culprits were `FOG_COLOR=0xA4B8DB` (pale blue) over a 0–1024-block linear blend, plus `SKY_COLOR=0x658CD7` (daytime blue). Both are now `0` while the gate is active.
- **Dome wall material on the south side is black_concrete**, not the `cyan_terracotta` that dominates the broader grid (118k cyan_terracotta vs ~51k black_concrete world-wide; locally near the south wall it's the other way around).
- **PeopleMover hole composition**: 1894 air cells + **141 invisible `Barrier`** + **9 invisible `Light`** blocks. The first naïve `isAir()` rule leaked through the barriers. Fix: `state.isAir() || state.is(Blocks.BARRIER) || state.is(Blocks.LIGHT)`.

## Tunables

Top of [SpaceMountainStarRenderer.java](src/main/java/com/chenweikeng/imf/nra/spacemountain/SpaceMountainStarRenderer.java):

```java
STAR_COUNT     = 1500
SPAWN_RADIUS   = 60.0    // blocks; covers the 80x80 dome interior with margin
STAR_SIZE_MIN  = 0.18f
STAR_SIZE_MAX  = 0.55f
SEED           = 0xCAFEBABEL
DOME_CENTER    = (-270, 80, 167)
```

Hole bbox is in [SpaceMountainBlockOverride.java](src/main/java/com/chenweikeng/imf/nra/spacemountain/SpaceMountainBlockOverride.java) (`HOLE_X_MIN/MAX`, `HOLE_Y_MIN/MAX`, `HOLE_Z_MIN/MAX`).

## Helper tooling

In `debug-dumps/` at the project root:

- **`/imf dumpchunks <radius>`** — in-game command (registered in `NotRidingAlertClient`, implemented in [ChunkDumpCommand.java](src/main/java/com/chenweikeng/imf/nra/spacemountain/ChunkDumpCommand.java)). Pure-Java iteration, no Lua bridge. Writes a single gzipped binary to `debug-dumps/chunks-<timestamp>.bin.gz`. Format documented in the file's javadoc.
- **`parse_dump.py`** — reference reader. Prints non-air bbox, top blocks, sections with `BL>=13` cells (likely emitters).
- **`dump_to_3d.py`** — converts a dump to `dome_blocks.json` (filtered to `(-270, 80, 167)` ± 80 horizontal, Y∈[30,120], with hand-curated colors for ~80 common Minecraft blocks).
- **`dome_viewer.html`** — Three.js viewer (r137 UMD, no build step). Controls: WASD move · Space/Shift up/down · R reset · mouse left-drag look, right-drag pan, scroll zoom · click a block to pick its coord. Right-panel UI: per-block-type checkboxes, Y cutaway sliders, hole-bbox toggle, speed slider. Open via `python3 -m http.server 8000` in `debug-dumps/` or via the Launch preview panel.

## Open items / next steps

1. **Verify the star renderer in-game** with the dome anchor in place. The previous "only one star" report was on the boarding-anchored version (radius 220). With the dome anchor (radius 60, count 1500) the cloud should engulf the interior. If only one star still appears, the bug is in the billboard-axis math in `drawStars` — first thing to check: whether `camera.rotation()` returns world-from-view (what the code assumes) or view-from-world (would need an inverse).
2. **Hide interior black-block clutter**. Pending specific coords from the user — can't blanket-hide `black_concrete` because the dome wall is made of it. The plan is a `Set<BlockPos>` of force-hide positions checked before the bbox-cover rule in `SpaceMountainBlockOverride.apply`.
3. **Singleplayer-world conversion** (proposed, not built). Workflow: `amulet-core` Python converter reads our dump and writes `.mca` files + `level.dat` so the user can walk the dome in 1.21.11 singleplayer and trim geometry. Caveat: dump has no block entities; only chunks loaded at dump time exist.

## Resuming

- Build/deploy: `./build-and-deploy.sh` from project root. Uses atomic-swap so a running JVM keeps its old jar (per the `feedback_deploy_script` memory).
- The mod's runtime DebugBridge port is **9876**; reconnect via the `mcdev-mcp` MCP and use `mc_execute` for live inspection (Lua bridge — keep loops to ≤ a few thousand calls per script to avoid hanging the websocket).
- New per-ride coords (e.g. for other rides): board the ride, run `/imf dumpchunks 12`, then `python3 dump_to_3d.py chunks-<timestamp>.bin.gz` and inspect in `dome_viewer.html`. The flood-fill pattern in earlier analysis runs is the canonical way to find a hollow's bbox from a chunk dump.
