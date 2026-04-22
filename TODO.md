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

### Stage 1 — Generator + chat display
- [x] `ImfStorage.nraDailyPlan()` path
- [x] `DailyPlan` / `DailyPlanNode` POJOs (with `completed` field reserved for Stage 2)
- [x] `DailyPlanStorage` load/save
- [x] `DailyPlanGenerator` — 5 singles, k=2, random distinct rides from filtered pool
- [x] `DailyPlanManager` — get-or-create today's plan, regenerate on date roll
- [x] `/rideplan` command prints tree in chat
- [x] `DailyPlanChatRenderer` — colored ASCII list

**Done when**: `/rideplan` shows 5 random rides with ×2 next to each; returns same plan across relog within the day; new plan next day.

### Stage 2 — Auto-completion + Tier-1 stamps
- [ ] Hook `RideCountManager` delta detection
- [ ] Mark nodes complete when delta ≥ k
- [ ] On node complete: chat line + `happy_villager` particles + `note_block.bell`
- [ ] Update `/rideplan` output to show ● / ◐ / ○
- [ ] Persist completion state

### Stage 3 — HUD breadcrumb
- [ ] Register `imaginemorefun:daily_plan` HUD layer above hotbar
- [ ] Single-line render: `⊙─⊙─●─○─○  next: Pirates ×2`
- [ ] Config toggle (default on)

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
