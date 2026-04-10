# ImagineMoreFun

A merged quality-of-life mod for the ImagineFun Minecraft server, bundling three previously-separate mods into a single Fabric 1.21.11 JAR:

- **Not Riding Alert** — ride tracking, strategy HUD, alerts, audio integration, daily reports, and more. See `com.chenweikeng.imf.nra.*`.
- **Pim!** — pin collection visual highlights, trader warping, and pin-book tooling. See `com.chenweikeng.imf.pim.*`.
- **SkinCache** — local caching of player skin textures to reduce Mojang CDN traffic. See `com.chenweikeng.imf.skincache.*`.

## Target

- Minecraft **1.21.11** (Fabric)
- Java 21

## Build

```bash
./gradlew build
# → build/libs/imaginemorefun-3.0.0.jar
```

## Commands

All three original command surfaces are preserved verbatim:

- `/nra`, `/nra ridereport`, `/oa connect|disconnect|reconnect|volume|…`
- `/pim:compute`, `/pim:trade`, `/pim:reset`, `/pim:export`, `/pim:value`, `/pim:fmv`

## Storage

- NRA data: `config/imaginemorefun/` (automatically migrated from `config/not-riding-alert*.json` on first launch)
- SkinCache data: `<gameDir>/skincache/` (unchanged from upstream)
- Pim has no disk state

## Upstream

This is a fork. The three upstream repos (`not-riding-alert`, `pim`, `skin-cache-mod`) are no longer tracked. See `LICENSE` for attribution.

## Layout

```
com.chenweikeng.imf                 — umbrella (ImfClient, ImfStorage, ImfMigration)
com.chenweikeng.imf.nra.*           — NRA sources (except mixins)
com.chenweikeng.imf.pim.*           — PIM sources (except mixins)
com.chenweikeng.imf.skincache.*     — SkinCache sources (except mixins)
com.chenweikeng.imf.mixin.*         — all mixin classes, flat, prefixed by origin
```

A single `imf.mixins.json` references every mixin under `com.chenweikeng.imf.mixin`.

## License

Mixed CC0-1.0 (NRA + PIM + umbrella) and MIT (skincache). See `LICENSE` and `LICENSE-CC0`.
