---
name: taste-design-polish
description: Design aesthetics, typography, micro-interactions, spacing, dark mode color grading, and impeccable visual craft. Inspired by Leonxlnx/taste-skill and pbakaus/impeccable.
version: 1.0.0
---

# Taste & Impeccable Design Polish Skill

## Purpose
Enforces Apple macOS / Linear-grade visual standards, eliminating generic AI bootstrap aesthetics, cramped margins, and poor color contrast.

## Standards & Checkpoints
1. **Typography & Hierarchy**:
   - Use curated fonts (Geist, Inter, SF Pro style) with distinct weights (SemiBold 600 for headers, Regular 400 for prose, Medium 500 for captions).
   - Strict scale: Headline (20-24sp), Title (16-18sp), Body (14sp), Caption/Label (11-12sp).
2. **Color Grading & Dark/Light Themes**:
   - Dark Mode: Deep Obsidian (`#08090B`), Card Surface (`#101216`), Hairline borders (`#262C36`). Never pure black `#000000` with pure white `#FFFFFF` text.
   - Light Mode: Crisp Paper (`#F8FAFC`), Card Surface (`#FFFFFF`), Border (`#CBD5E1`).
   - Contrast: Maintain WCAG AA >= 4.5:1 text-to-background contrast at all times.
3. **Micro-Interactions & Touch Feedback**:
   - Provide tactile haptic feedback on destructive or state-changing actions (`HapticFeedbackType.LongPress` or `EFFECT_CLICK`).
   - Smooth transition curves (`FastOutSlowInEasing`, 150–200ms).
4. **Information Architecture**:
   - Generous breathing room (padding: 12–16dp, gap: 8–12dp).
   - Card surfaces with subtle elevation or 1dp boundary outlines.

