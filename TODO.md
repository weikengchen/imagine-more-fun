# Daily Ride Plan — implementation roadmap

Living document. Tick stages off as they ship; revise scope based on in-game feedback.

Design reference: `/Users/cusgadmin/Downloads/compass_artifact_wf-ccdf816b-1bc7-4a31-baab-3498979a227e_text_markdown.md` (not checked into the repo).

## Decisions locked in

- **Seed**: fresh random on first generation of each local day. Not date-hashed, not shared.
- **Credit model**: prospective-only. Snapshot ride counts at generation time; nodes credit only against the delta since.
- **Scope**: purely local, single-player's own goals. No UUID suffix, no per-profile split — one file at `config/imaginemorefun/nra-daily-plan.json`.
- **Reset**: local system midnight, matching `/ridereport` (`LocalDate.now()`).
- **Filters** (same as strategy HUD): honor `hiddenRides`, `onlyAutograbbing`, exclude rides at or past `maxGoal`.

## Stages

### Stage 1 — Generator + chat display ✅ shipped (0629029)
- [x] `ImfStorage.nraDailyPlan()` path
- [x] `DailyPlan` / `DailyPlanNode` POJOs (with `completed` field reserved for Stage 2)
- [x] `DailyPlanStorage` load/save
- [x] `DailyPlanGenerator` — 5 singles, k=2, random distinct rides from filtered pool
- [x] `DailyPlanManager` — get-or-create today's plan, regenerate on date roll
- [x] `/rideplan` command prints tree in chat
- [x] `DailyPlanChatRenderer` — colored ASCII list

**Done when**: `/rideplan` shows 5 random rides with ×2 next to each; returns same plan across relog within the day; new plan next day.

**How to test Stage 1**
1. Launch ImagineFun via Modrinth launcher; the new JAR is already deployed.
2. Join any world/server — `/rideplan` is client-only and not gated to ImagineFun.
3. Run `/rideplan`. Expect: gold divider, "Today's Ride Plan" header with date, 5 numbered rides with `×2` in cyan.
4. Run `/rideplan` again. Expect: **identical** 5 rides (same file, same plan).
5. Inspect `~/Library/Application Support/ModrinthApp/profiles/ImagineFun/config/imaginemorefun/nra-daily-plan.json` — should contain today's ISO date, epoch ms, a full `snapshotCounts` map, and 5-node list.
6. Toggle `onlyAutograbbing` on in the mod config, **delete the plan file**, run `/rideplan`. Expect: rides drawn only from the autograbbing set.
7. Roll the day: either delete the file or advance system clock past midnight, run `/rideplan`. Expect: fresh 5 rides and a new date in the file.
8. Edge case: set `maxGoal` low, ride everything past it, delete plan. Expect: header says "No eligible rides".

### Stage 2 — Auto-completion + Tier-1 stamps ✅ shipped
- [x] Hook `RideCountManager` delta detection (per-tick poll from `NotRidingAlertClient.onClientTick`)
- [x] Mark nodes complete when delta ≥ k
- [x] On node complete: chat line + `happy_villager` particles + `note_block.bell`
- [x] Update `/rideplan` output to show ● / ◐ / ○ with live `m/k` badge
- [x] Persist completion state (saved to `nra-daily-plan.json` on change)
- [x] Plan-complete flourish: level-up sound + double particles + "Daily Ride Plan complete!" line

**How to test Stage 2**
1. Launch ImagineFun with the new JAR.
2. Run `/rideplan` to see today's plan (any already-completed nodes will render with `●`).
3. Ride one of the listed rides. Expect after each completion increment:
   - Chat: `✨ [IMF] Node complete! <ride name> (2/5)`
   - Note-block bell sound
   - Burst of 12 green `happy_villager` particles around your head
4. Run `/rideplan` again — that node now renders with `●` in green, others still `○`.
5. Complete all 5 nodes → expect extra "Daily Ride Plan complete!" line + level-up sound + 24 particles.
6. Relog mid-way → completed nodes persist; progress on incomplete nodes comes from live count delta against the saved snapshot.

### Stage 3 — Top-of-screen tree HUD ✅ shipped
- [x] Register `imaginemorefun:daily_plan` HUD layer (attached before CHAT, same as strategy)
- [x] Horizontal left-to-right chain at top of screen: title + centered node chain
- [x] Each node = glyph (●/◐/○) + ride short name + `m/k` progress, connectors `──` between
- [x] Replaces the strategy HUD while active (strategy dispatcher early-returns when plan HUD is active)
- [x] Config toggle `showDailyPlanHud` (default on) under General
- [x] Respects existing `trackerDisplayMode` + boss-bar suppression (inherits strategy HUD's gating)

### Stage 3.5 — Tree-like boxes + backdrop ✅ shipped
- [x] Semi-transparent panel (`0xB0000000`) behind the whole HUD for contrast against gameplay
- [x] Each node rendered as a bordered box (filled `0x60000000` + 1px border in status color)
- [x] Two rows inside each box: `● NAME` top, `m/k` bottom, both centered
- [x] Proper horizontal connector lines (2px thick, drawn via `hLine`) between node boxes
- [x] Connector line colored green when the source node is complete — "energized skill-tree" feel

### Stage 3.6 — Riding countdown row ✅ shipped
- [x] When riding (`CurrentRideHolder.getCurrentRide() != null`), render a row between title and chain: `▶ <ride> · <pct>% · <m s> left` in `trackerRidingColor`
- [x] When autograbbing (autograb ride at location & not yet a passenger), render `⟲ Autograbbing <ride>…` in `trackerAutograbbingColor`
- [x] Panel height grows automatically when the row is present, shrinks back when not

**How to test Stage 3**
1. Join ImagineFun with new JAR.
2. Expect top-of-screen row: `✨ Ride Plan · Thu Apr 23 · 0/5` (gold), below it a centered chain `○ ALICE 0/2 ── ○ BTM 0/2 ──` … with untouched nodes in gray.
3. Ride one ride once → chain updates: `◐ ALICE 1/2` in orange. Ride twice → bell + particles fire, chain line flips `● ALICE ×2` in green.
4. Strategy HUD should no longer be visible while plan HUD is showing.
5. Open Cloth config → General → toggle "Show Daily Ride Plan HUD" off → strategy HUD returns, plan HUD vanishes.
6. Summon a boss (or any vanilla boss bar) → plan HUD hides cleanly.
7. `trackerDisplayMode=ONLY_WHEN_RIDING` → plan HUD hides when not riding, mirroring strategy behavior.
8. All 5 nodes done → title flips green, chain is all-green `●`s.

### Stage 4 — Generator v2: branching
- [ ] OR nodes (~40%), one AND gate, one 2-of-3 capstone
- [ ] Constraint solver: no repeat within 2 layers, novelty slot, AND k-symmetry
- [ ] Update chat + HUD renderers for branches

### Stage 5 — Screen UI
- [ ] `DailyPlanScreen` with vertical L-bend layout
- [ ] States: locked / available / in-progress / complete
- [ ] `J` keybind + `/rideplan open`
- [ ] Sprite atlas at `assets/imaginemorefun/textures/gui/tree_icons.png`

### Stage 6 — Passport persistence
- [ ] `passport.json` accumulates per-ride stamp counts + day history
- [ ] Populate from Stage 2 onward (backfill not attempted)
- [ ] No UI yet

### Stage 7 — Side quests + Ambassador voice
- [ ] Side-quest node bank (~15 to start)
- [ ] 2–3 side-quest branches per tree
- [ ] Tier-2 flourish on completion
- [ ] `~15` Ambassador lines, 90s rate limit

## Deferred (post Stage 7)

Passport Screen, Hidden Mickeys, titles, Fast Passes, Heat/opt-in difficulty, mood selector,
Yesterday's Echo, weekly summary in `/ridereport`, `/randomride tree` flag.
