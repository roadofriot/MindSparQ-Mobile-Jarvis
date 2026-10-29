# 04 — DRD: Design Requirements Document

Source of truth: `Docs/Stitch/obsidian_kinetic/DESIGN.md` + Stitch HTML. This document turns it into implementable Compose rules and fixes inconsistencies.

## 1. Design principles
1. **Signal over decoration** — glow and colour only communicate state (running, passing, failing, focus).
2. **Truthful UI** — never show numbers we did not measure. Unknown = `—`.
3. **Two typefaces, two worlds** — Inter for human language, JetBrains Mono for machine state.
4. **Progressive disclosure** — advanced panels (MCP, routing, graph) collapsed by default.
5. **Thumb-first on phone, keyboard-first on PC** — every primary action reachable in the bottom 40 % on phones; every action has a shortcut on Desktop.

## 2. Token reconciliation

`DESIGN.md` frontmatter (M3 roles, surface `#10141a`) and prose (obsidian `#07090E`, slate `#0D1117/#161B22`) disagree. Decision:

| Role | Dark | Light | Notes |
|---|---|---|---|
| `background` (Tier 0) | `#07090E` | `#F6F8FA` | prose value |
| `surface` (Tier 1) | `#0D1117` | `#FFFFFF` | cards |
| `surfaceContainer` (Tier 2 base) | `#161B22` | `#F0F3F6` | sheets (glass on Desktop) |
| `surfaceContainerHigh` | `#262A31` | `#E6EAEF` | frontmatter |
| `outline` hairline | `#FFFFFF` @ 8 % | `#1F2328` @ 10 % | |
| `onSurface` | `#F0F6FC` | `#1F2328` | |
| `onSurfaceVariant` | `#8B949E` | `#59636E` | |
| telemetry text | `#6E7681` | `#6E7781` | |
| `primary` (agent/active) | `#00F0FF` | `#00838F` | light variant darkened for 4.5:1 |
| `onPrimary` | `#07090E` | `#FFFFFF` | |
| `secondary` (git/context) | `#6366F1` | `#4F46E5` | |
| `tertiary` (pass/healthy) | `#00E5A3` | `#00875F` | |
| `error` | `#FFB4AB` / container `#93000A` | `#BA1A1A` | |
| `warning` (new) | `#F5C451` | `#9A6700` | approvals, cooldown keys |

Light theme is required by the user rules; mocks are dark-only, so light values above are derived and must pass contrast checks.

**Typography** — as frontmatter (`headline-lg 32/40` … `label-sm 10/14`). Bundle Inter + JetBrains Mono (OFL) as Compose resources. Tabular numerals (`fontFeatureSettings = "tnum"`) for all telemetry.

**Shape** — 8 dp components, 16 dp cards, 24 dp top corners on sheets, pills only for status.

**Spacing** — 4-dp grid: xs 4, sm 8, md 12, lg 16, xl 24; screen margin 16 dp.

**Motion** — 150–200 ms `FastOutSlowIn` for state changes; streaming pulse 1.2 s loop only while a task runs; respect system "remove animations".

**Glass** — Desktop & Android 12+: `Modifier.blur`-backed glass for Tier 2/3. Android < 12: solid `#161B22` @ 95 %. (Performance: no blur on list items.)

**Icons** — Lucide icon set (ISC licence) via `composables-icons-lucide` or vendored SVG → ImageVector. Material Symbols names in mocks map to Lucide equivalents (table in `designsystem/IconMap.kt`).

## 3. Layout system

| Width class | Layout |
|---|---|
| Compact (< 600 dp) — phones | Single pane; bottom nav (5 tabs); composer docked above keyboard; sheets for secondary panels |
| Medium (600–1024) — foldables/tablets | Two panes: leading = Studio/terminal, trailing = Hub chat; nav rail |
| Expanded (> 1024) — Desktop | Three regions: left nav rail + project tree, centre editor/preview, right agent panel; bottom dock (terminal/tests/logs) resizable; top bar with project switcher, branch, run controls, model chip |

Navigation destinations (from mocks): **Hub · Plan · Studio · Sim · Settings**. Projects list is the start destination before a project is opened (and a project switcher in the top bar afterwards).

## 4. Component library (`core/designsystem`)

| Component | Spec |
|---|---|
| `JButtonPrimary` | 40 dp (48 dp touch target), cyan fill, obsidian text; `running` state = animated border sweep |
| `JButtonSecondary` | Tier-2 glass, 1 px hairline |
| `JIconButton` | 36 dp visual / 48 dp touch, 18 dp stroke icon, tooltip (Desktop hover, Android long-press) |
| `JChip` / `JStatusPill` | 24 dp, mono `label-sm`, domain tint (cyan agent, indigo git, emerald runtime, amber warning, red error) |
| `JCard` | Tier-1, header with mono file path + status dot |
| `JCodeBlock` | `#07090E` background, horizontal scroll, copy button, line numbers optional |
| `JDiffView` | unified (compact) / split (expanded); hunk accept/reject |
| `JTerminal` | mono 12 sp, ANSI colours (16 + 256), selection & copy, scroll-lock toggle |
| `JComposer` | prefix glyph: `✦` agent, `>` terminal; attachments row; mic; send/stop toggles |
| `JTimeline` | task events with icon per evidence kind, monospace timestamps, expandable details |
| `JApprovalCard` | amber border; title, risk level, preview (diff/command), buttons Approve / Edit / Deny / "Always allow this pattern" |
| `JModelChip` | provider icon + model name + health dot; tap opens model picker sheet |
| `JKeyHealthRow` | label, masked key `AIza…f3Q`, state pill, cooldown countdown, requests today |
| `JEmptyState`, `JErrorState`, `JSkeleton` | for every list |
| `JToggle` | 36×20 capsule |
| `JSheet` | 24 dp top radius, drag handle, Tier-3 on Desktop as dialog |

## 5. Mock → real screen mapping

| Stitch screen | Real screen(s) | Kept | Changed/removed |
|---|---|---|---|
| Command Hub | **Hub** | composer, mic, attach, presets row, agents panel, notifications | "Synthesize" → "Send"; sub-agent list shows only real running tasks; camera button = image attachment |
| Project Planner / Spec Studio | **Plan** | spec tree, model chips, decision chips, "Synthesize Complete Project Specs", new_spec.md, diff preview | "AST VERIFIED" removed; token counter shows real context usage |
| Architecture Studio | **Studio → Graph tab** | node graph, diff, branch, export, instruct input | nodes come from code graph/stack detection; "LIVE AST" removed |
| Live Simulator | **Sim → Preview** | viewport presets, reload, inspector toggle (web only) | iPhone frames → generic presets; "Touch Inspector" = DOM inspector for web preview |
| Deploy Hub | **Sim → Pipeline** | stage list, log stream, copy/share/filter, targets | Targets = Debug / Release / GitHub Release; TestFlight removed |
| Profile & Settings | **Settings** | model routing, quotas (real usage), preferences (verbosity), storage actions | identity/crypto session removed; force-push moved to Git |

## 6. Accessibility & i18n
- All icon buttons have `contentDescription` (en + ne strings).
- Min contrast 4.5:1 body, 3:1 large text/icons; verified with a unit test computing contrast for every token pair.
- Font scaling up to 200 % without clipping on Hub and Settings.
- Strings in Compose Resources `values/` (en) and `values-ne/` (Nepali). Numbers use Latin digits in code/telemetry; Devanagari digits optional in prose.

