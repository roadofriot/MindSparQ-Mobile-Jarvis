---
name: Obsidian Kinetic
colors:
  surface: '#10141a'
  surface-dim: '#10141a'
  surface-bright: '#353940'
  surface-container-lowest: '#0a0e14'
  surface-container-low: '#181c22'
  surface-container: '#1c2026'
  surface-container-high: '#262a31'
  surface-container-highest: '#31353c'
  on-surface: '#dfe2eb'
  on-surface-variant: '#b9cacb'
  inverse-surface: '#dfe2eb'
  inverse-on-surface: '#2d3137'
  outline: '#849495'
  outline-variant: '#3b494b'
  surface-tint: '#00dbe9'
  primary: '#dbfcff'
  on-primary: '#00363a'
  primary-container: '#00f0ff'
  on-primary-container: '#006970'
  inverse-primary: '#006970'
  secondary: '#c0c1ff'
  on-secondary: '#1000a9'
  secondary-container: '#3131c0'
  on-secondary-container: '#b0b2ff'
  tertiary: '#d9ffe9'
  on-tertiary: '#003825'
  tertiary-container: '#34f6b2'
  on-tertiary-container: '#006d4b'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#7df4ff'
  primary-fixed-dim: '#00dbe9'
  on-primary-fixed: '#002022'
  on-primary-fixed-variant: '#004f54'
  secondary-fixed: '#e1e0ff'
  secondary-fixed-dim: '#c0c1ff'
  on-secondary-fixed: '#07006c'
  on-secondary-fixed-variant: '#2f2ebe'
  tertiary-fixed: '#43ffbb'
  tertiary-fixed-dim: '#00e2a0'
  on-tertiary-fixed: '#002114'
  on-tertiary-fixed-variant: '#005138'
  background: '#10141a'
  on-background: '#dfe2eb'
  surface-variant: '#31353c'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 26px
    fontWeight: '600'
    lineHeight: 34px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.015em
  headline-sm:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '500'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  code-lg:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  code-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 18px
  code-sm:
    fontFamily: JetBrains Mono
    fontSize: 11px
    fontWeight: '400'
    lineHeight: 16px
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.04em
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.06em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 0.75rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1rem
  space-xl: 1.5rem
---

## Brand & Style

This design system targets autonomous software engineers, platform architects, and systems developers operating complex agentic workflows directly from high-density mobile interfaces. The emotional profile is calm, authoritative, razor-sharp, and unyielding—evoking a synthetic intelligence co-pilot operating in deep space. 

The aesthetic merges precision technical minimalism with controlled glassmorphism and subtle luminescent instrumentation. Elements emphasize high signal-to-noise ratios, sub-pixel alignments, and optical hierarchy. Visual weight is derived through subtle value shifts in dark slate and obsidian rather than heavy ornamental chrome. Surfaces feel machined, translucent, and alive with restrained kinetic feedback.

## Colors

The foundation is built on absolute dark obsidian (`#07090E`) graduating into structured slate layers (`#0D1117` and `#161B22`). Luminance is strictly functional:

- **Primary (`#00F0FF` - Electric Cyan):** Reserved for agent execution states, active execution streams, cursor focus, and critical interactive targets.
- **Secondary (`#6366F1` - Muted Indigo):** Context switches, AST node relationships, branch states, and non-blocking autonomous actions.
- **Tertiary (`#00E5A3` - Synthetic Emerald):** Unit test passes, healthy pipeline telemetry, and secure cryptographic validations.
- **Neutral (`#0D1117` to `#F0F6FC`):** Strict slate spectrum. Backgrounds employ ultra-deep dark values, borders utilize fine-line alpha overlays (`rgba(255, 255, 255, 0.08)`), and text scales from crisp high-contrast off-white (`#F0F6FC`) down to subdued telemetry slate (`#6E7681`).
- **Accent Glows:** Glows are tightly bound to bounding perimeters using 8px–16px blurs at 15–25% alpha, eliminating ambient wash while maintaining instrument-grade focus.

## Typography

Typography enforces a strict bifurcated system: human natural-language processing vs. machine execution syntax.

- **Prose & Structural Narrative (`Inter`):** Handles conversation with the AI core, architectural prompts, documentation, and interface chrome. Tight tracking (`-0.01em` to `-0.02em`) preserves visual sharpness on high-DPI OLED screens.
- **Syntactic & Machine State (`JetBrains Mono`):** Dedicated to diff views, execution logs, file hierarchies, tokens, runtime telemetry, and micro-labels. Tabular figures are enforced system-wide for alignment of real-time operational data.
- **Scale Continuity:** Font scaling on mobile prioritizes maximum structural density without degradation of legibility. Line heights follow an exact 4px grid multiple.

## Layout & Spacing

The layout is architected around an 8-column mobile-first fluid layout grid paired with rigid 4px/8px incremental rhythm units. 

- **Outer Enclosure:** Outer canvas margins maintain `1rem` (16px) on mobile viewports, clamping content safely away from native edge swipe triggers and display cutouts.
- **Rhythm & Density:** Code editing interfaces and agent runtime logs contract to `space-xs` (4px) and `space-sm` (8px) gaps to optimize visual context window. Chat and conversational scaffolding expand to `space-md` (12px) and `space-lg` (16px) to maintain readable breathing room.
- **Adaptive Breakpoints:** Handheld mobile views (<600px) use the single-pane 8-column container. Foldables and tablets (600px–1024px) branch into a dual-pane master-detail model, anchoring the terminal/code surface on the leading rail and agent dialogue on the trailing pane.

## Elevation & Depth

Depth is established through translucent physical tiers, dark surface luminance, and sub-pixel perimeter lighting rather than diffused atmospheric drop shadows.

- **Surface Tier 0 (Canvas Base):** Deepest ground (`#07090E`). Solid, non-reflective.
- **Surface Tier 1 (Base Cards & Containers):** `#0D1117` with a 1px border of `rgba(255, 255, 255, 0.06)`. Flat, grounded.
- **Surface Tier 2 (Floating Code Panes & Sheets):** High-density glass (`rgba(22, 27, 34, 0.75)`) backed by a `backdrop-filter: blur(16px) saturate(180%)`, framed by a top-edge highlight (`rgba(255, 255, 255, 0.12)`) and bottom edge containment (`rgba(0, 0, 0, 0.4)`).
- **Surface Tier 3 (Modals & Command Palettes):** Layered obsidian (`rgba(13, 17, 23, 0.9)`) with an inner 1px border glowing subtly with primary alpha (`rgba(0, 240, 255, 0.2)`), elevated over backdrop dimming (`rgba(0, 0, 0, 0.7)`).
- **Active State Glow:** Focused interactive items or actively writing agent nodes emit a constrained dual shadow: `0 0 0 1px #00F0FF, 0 4px 20px rgba(0, 240, 255, 0.15)`.

## Shapes

The interface balances the organic ergonomics of modern mobile hardware with the precision of developer IDEs via refined intermediate roundedness.

- **Primary Shapes (`roundedness: 2`):** Base components, chips, code line highlight blocks, and inner inputs utilize a baseline `0.5rem` (8px) corner radius.
- **Structural Groupings (`rounded-lg`):** Cards, terminal frames, inspector drawers, and syntax context blocks scale to `1rem` (16px).
- **Modal Containers (`rounded-xl`):** Bottom-anchored execution sheets, REPL drawers, and system overlays use `1.5rem` (24px) top-corner radii.
- **Micro Tokens:** Pill geometry is restricted solely to status indicators (e.g., branch status, live port badges, agent operational state capsules).

## Components

### Buttons & Action Bars
- **Primary Execution Button:** Cyan base (`#00F0FF`) with pure obsidian text (`#07090E`), medium font weight, 40px mobile touch height. In active generation states, applies an animated linear gradient pulse along its border.
- **Secondary Tooling Button:** Tier 2 slate glass background with 1px border (`rgba(255, 255, 255, 0.08)`), text in high-contrast off-white (`#F0F6FC`).
- **Compact Icon Buttons:** 36x36px optical touch surface with centered 18px SVG stroke icons. Features immediate haptic feedback and inline micro-tooltips (`label-sm` in `JetBrains Mono`) anchored dynamically to touch-up release points.

### Cards & Code Containers
- Cards implement Material 3 spatial logic reimagined in dark glass. Background uses `rgba(22, 27, 34, 0.7)`. 
- Headers feature segmented filepaths rendered in `label-md` with leading status dots (green for compiled, cyan for executing, red for syntax error).
- Inner code containers enforce absolute horizontal scroll locks with background tinting (`#07090E`) to visually dissociate code blocks from natural conversation text.

### Chips & AST Node Pills
- Compact 24px height. Border radius of 12px.
- Background uses subtle tint of the associated domain: Cyan for AI agents, Indigo for Git topology, Emerald for runtime execution.
- JetBrains Mono `label-sm` labels accompanied by 10px status dot or vector glyph.

### Form Inputs & Terminal Command Fields
- Inset styling with background `#07090E` and continuous 1px border (`rgba(255, 255, 255, 0.08)`).
- Prefix iconography denotes engine mode (e.g., `>` for terminal commands, `✦` for Jarvis natural prompt execution).
- Focus state activates cyan edge illumination (`#00F0FF`) with ambient micro-glow (`0 0 12px rgba(0, 240, 255, 0.2)`).

### Lists & Telemetry Feeds
- Edge-to-edge layout separated by sub-pixel hairline dividers (`rgba(255, 255, 255, 0.04)`).
- Active execution items feature an animated trailing spark line and monospace timestamps on the right margin.

### Checkboxes, Radios & Toggles
- Checkboxes maintain a compact 16x16px bounding box with 4px corner radius. Active fill is `#00F0FF` with `#07090E` checkmark.
- Toggles utilize a sleek 36x20px capsule track containing a 16px sliding thumb, shifting from `#161B22` (inactive) to glowing `#00F0FF` (engaged).

### Prompt Streaming Indicator
- Dedicated component anchored above the keyboard: dynamic glass ribbon rendering real-time token throughput metrics (`tokens/sec`, AST depth) and a glowing audio-visualizer kinetic wave during voice-to-code dictation.