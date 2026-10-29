---
name: project-discovery
description: Rapid inspection of project structure, entry points, dependencies, and configuration.
tools:
  - list_dir
  - read_file
  - search_code
---

# Project Discovery Skill

## Purpose
Quickly maps out the structure of a new or unfamiliar codebase without reading every file, conserving token budgets.

## Step-by-Step Procedure
1. **List Root Directory**: Call `list_dir` on the project root to detect package managers (`package.json`, `pubspec.yaml`, `build.gradle`, `requirements.txt`, `Cargo.toml`).
2. **Identify Primary Language & Framework**:
   - Flutter / Dart: `pubspec.yaml`, `lib/main.dart`
   - Android / Kotlin: `build.gradle.kts`, `AndroidManifest.xml`
   - Python: `pyproject.toml`, `setup.py`, `app.py`
   - React / Web: `package.json`, `vite.config.ts`, `src/App.tsx`
3. **Inspect Configuration & Entry Points**: Read the main configuration file and locate entry points.
4. **Output Findings**:
   - Technology Stack & Detected Framework
   - Build & Test Commands Available
   - Key Entry Files

