---
name: technical-decision-records
description: Author and maintain Architecture Decision Records (ADR) to document significant architectural and engineering decisions.
tools:
  - read_file
  - write_file
  - list_dir
---

# Technical Decision Records (ADR) Skill

## Purpose
Establishes a verifiable, auditable record of significant technical, structural, and architectural choices, capturing context, consequences, alternatives considered, and validation criteria.

## When to Use
- Selecting a new database, network protocol, or runtime dependency.
- Migrating from one architecture pattern to another (e.g., SharedPreferences to SQLDelight).
- Establishing cross-platform boundaries, security policies, or concurrency paradigms.
- Any decision that is hard to reverse and has cross-cutting implications.

## Structure of an ADR
Each ADR must be stored in `docs/architecture/decisions/` or `docs/adr/` with the filename `ADR-{NUMBER}-{SLUG}.md`:

```markdown
# ADR-{NUMBER}: {Title of the Decision}

- **Status**: [Proposed | Accepted | Superseded | Deprecated]
- **Date**: YYYY-MM-DD
- **Deciders**: {Architects, Engineers}
- **Technical Story**: {Context or Issue Reference}

## Context and Problem Statement
What is the problem being solved? What forces, constraints, and business needs are in play?

## Decision Drivers
- Performance, latency, memory consumption (e.g., dual-core i3 / 10GB RAM constraint)
- Security and secret protection (e.g., Keystore AES-256-GCM vs plaintext)
- Offline-first resilience and zero data loss
- Maintenance burden and dependency hygiene

## Considered Options
1. Option A (e.g., SharedPreferences JSON blobs)
2. Option B (e.g., Room ORM with KSP)
3. Option C (e.g., SQLDelight multiplatform with explicit B-trees)

## Decision Outcome
Chosen option: **Option C** because...

### Positive Consequences
- Strict compile-time SQL verification
- Minimal memory footprint and multiplatform portability
- Foreign key constraints with cascading deletes

### Negative Consequences / Trade-offs
- Manual migration scripts required for schema changes
- Slightly steeper learning curve for raw SQL queries

## Verification & Validation Plan
How was or will this decision be verified?
- Unit test execution (`./gradlew testDebugUnitTest`)
- Memory profiling under low-spec conditions
- Real device execution on physical hardware
```

