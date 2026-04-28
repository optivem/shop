# Ticket Classification + Legacy Coverage Cycle

Conversation date: 2026-04-28. Session paused before edits because two new sibling agents (`diagram-tweaker`, `diagram-content-editor`) were created mid-session and are not yet in Claude Code's startup-loaded agent registry. Restart Claude Code to dispatch them, then resume from this plan.

## Why

The current process diagram (`docs/atdd/process/process-diagram.md`) and orchestrator prose (`docs/atdd/process/orchestrator.md`) only describe two agents in intake — `atdd-story` and `atdd-bug` — but two more exist as agent definitions: `atdd-task` and `atdd-chore`. Those are not behavioral changes and don't belong on the AT Cycle path; they need their own cycle and their own classification logic. The diagram + prose need to catch up.

## Operational taxonomy (agreed)

The dividing line is **does the change modify the system's acceptance criteria?**

- **Behavioral change** = a change that modifies the acceptance criteria. Acceptance tests change.
  - `atdd-story` → new behavior.
  - `atdd-bug` → restored behavior.
  - Routes to → **AT Cycle** (existing).
- **Structural change** = a change that preserves the acceptance criteria. Acceptance tests stay the same.
  - `atdd-task` = **interface change** at the system boundary (system API, system UI, external system API). Driver *implementations* update to match the new interface; driver *interfaces* stay the same so existing acceptance tests still pass through them.
  - `atdd-chore` = **internal-only change** inside the system (refactor a class, rename, dependency upgrade). No boundary change. Drivers untouched.
  - Routes to → **Legacy Coverage Cycle** (NEW subprocess).

### Legacy Coverage Cycle discipline (high-level only — internal phases TBD)

- Acceptance criteria are preserved by construction.
- All existing acceptance tests must stay green.
- A new test is added **only if the change exposes a coverage gap**.
- For `atdd-task`, driver bodies adapt to the new interface.
- For `atdd-chore`, drivers are untouched.

The user will define the cycle's internal phases in a follow-up turn — do not invent them.

## Plan placement (agreed)

- `docs/atdd/process/glossary.md` — add term entries for: Behavioral change, Structural change, Interface change, Internal-only change, Legacy Coverage Cycle. Keep entries to 1–2 sentences each, using the operational definitions verbatim where you can.
- `docs/atdd/process/orchestrator.md` — extend the existing **Intake (per ticket)** section so the routing table covers all four agent types and the prose explains the behavioral-vs-structural classification and the two cycles it routes into.
- **No new file.** The user explicitly chose not to create a new doc (e.g. `legacy-coverage-cycle.md`) for now — keep the prose in glossary + orchestrator. Revisit if the cycle's prose grows enough to warrant its own file.

## Diagram edits (process-diagram.md)

Use `diagram-content-editor` (after Claude Code restart) — scope: `process`. The agent will read diagram + prose, locate passages, apply surgical edits, prose-sync by default.

### Edit 1 — Intake block

Currently the Intake block has only a story/bug branch off `CLASSIFY{Ticket type classified by atdd-manager}`. Extend it to four branches:

- `atdd-story` (existing) → STOP_INTAKE → AT Cycle.
- `atdd-bug` (existing) → STOP_INTAKE → AT Cycle.
- `atdd-task` (new) → STOP_INTAKE → Legacy Coverage Cycle.
- `atdd-chore` (new) → STOP_INTAKE → Legacy Coverage Cycle.

The "Proceed to AT Cycle" boundary node currently lives at the bottom of Intake — add a sibling "Proceed to Legacy Coverage Cycle — see § Legacy Coverage Cycle" boundary node for the structural branch.

### Edit 2 — Overview block

Currently routes `INTAKE -->|STOP for approval, then per scenario| AT_CYCLE`. Update to:

- Add `LEGACY_CYCLE[Legacy Coverage Cycle — see § Legacy Coverage Cycle]` node.
- `INTAKE` now routes to **either** `AT_CYCLE` (behavioral) **or** `LEGACY_CYCLE` (structural), labelled accordingly.
- `LEGACY_CYCLE` flows into `DONE([All scenarios GREEN])` — same terminal — or whatever terminal makes sense once the cycle's internals are defined. For the stub, point it at `DONE` (placeholder).

### Edit 3 — New `## Legacy Coverage Cycle` detail block

Add a new H2 section under the existing detail blocks. Stub flow until phases are defined:

```
START([Triggered: ticket type = task or chore]) → LEGACY_COVERAGE_CYCLE[Legacy Coverage Cycle — phases TBD] → END([Done])
```

Keep it deliberately small — the user owns the internals.

## Refinements not yet applied (raised during the conversation, awaiting input)

The user added two refinements *after* agreeing to the base plan above:

1. **Separate "type of change" classification diagram** — a new detail block showing the behavioral-vs-structural decision explicitly, distinct from Intake's agent-typing decision. The classification diagram would show `Ticket → Behavioral or Structural? → ...`. Open question: does this replace the Intake block's classification, or sit alongside it?
2. **Per-scenario AT Cycle on the behavioral path** — the user wants the diagram to show that, on the behavioral side, a list of acceptance criteria expands into one AT Cycle execution per scenario. The existing Scenario Loop block already implements per-scenario looping at the bottom of the Overview; the question is whether the user wants this made more prominent at the classification level, or whether the existing Scenario Loop is sufficient.
3. **Structural cycle internals** — explicitly TBD; the user said *"for that I'll explain when you're ready."*

Do not act on these refinements until the user clarifies. Apply only the base plan (Edits 1–3 + glossary/orchestrator prose), then surface the open refinement questions.

## Promotion / contract

This is `diagram-content-editor`'s default content-edit + prose-sync flow. No opt-out signal in the user's prompt. Apply both diagram and prose edits in the same turn.

## Suggested invocation prompt (after restart)

```
Scope: process. Apply the base plan in plans/20260428-1500-ticket-classification-and-legacy-coverage-cycle.md (Edits 1–3 plus the glossary/orchestrator prose). Do NOT act on the "Refinements not yet applied" section. Stub the Legacy Coverage Cycle's internal phases with a single TBD node.

Prose files (named up-front so step 1 of your workflow does not need to STOP and ask):
- docs/atdd/process/glossary.md — add term entries for Behavioral change, Structural change, Interface change, Internal-only change, Legacy Coverage Cycle.
- docs/atdd/process/orchestrator.md — extend the existing "Intake (per ticket)" section so the routing table covers all four agent types (story, bug, task, chore) and the prose explains the behavioral-vs-structural classification + the two cycles it routes into.

No new prose file. Do not create legacy-coverage-cycle.md.
```

## Why this lives in plans/ instead of memory

The work is project-specific, multi-step, and pending — it belongs in a plan, not in long-term user/feedback memory. Once the edits are applied and the user is happy, this plan file can be deleted.
