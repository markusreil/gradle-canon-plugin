# 04 — Root → child extension propagation (core-owned copy engine)

Status: **in review** — no code until maintainer approves this plan.

## Goal

Propagation becomes a core feature instead of N contributor implementations: at root `afterEvaluate`, core walks
the `canon` tree and copies values root → children. Semantics come from an explicit `@Propagate` annotation when
present, otherwise from the property's type via a fixed table; types the table doesn't know fail fast. Contributors
write no copy code and need no core classes. This founds the `canon-api` module: the annotations are its first
genuinely shared types.

## Non-goals (explicitly deferred)

- Migrating holders to managed `Property` types (timing-based copy needs no laziness).
- DEEP merge implementation (see D5).

## State inventory (what there is to propagate)

- `canon` itself: empty holder, nothing to copy.
- `versioning.scheme` (`String`, default `"semver"`): explicitly `@Propagate(IMMUTABLE)` — project-wide, locked
  by verification. (A plain `String` is deliberately absent from the type table, so versioning-like plain state
  always requires an explicit mark.)
- `versioning.parsedVersion`: immutable by derivation — always computed from `rootProject.version`, no setter,
  not a property the pass sees; a child-local `version` never leaks into it.

## Design

New module `plugins/canon-api` (group `com.mreil.canon`, Kotlin, stdlib-only):

```kotlin
enum class CopySemantics { SHALLOW, DEEP, IMMUTABLE }

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Propagate(val value: CopySemantics = CopySemantics.SHALLOW)
```

Core and every contributor depend on `canon-api` only (contributors still never touch core impl). Notes on the
shape: the annotation targets the backing *field* (so contributors write a plain `@Propagate(...)` with no
use-site target) and the engine uses plain JDK field reflection — no kotlin-reflect (see D6). Core's copy pass,
registered on root `afterEvaluate`, considers each non-private, non-synthetic, non-static field of every `canon`
sub-extension in every project except root:

1. Effective semantics = explicit annotation value if present, else the type table (D3); unknown public types
   throw a clear error naming the project, property, and type.
2. SHALLOW: write the root value into the child field (children are still pristine — child scripts run later).
3. DEEP: rejected with "not implemented yet" until its chunk (see D5).
4. IMMUTABLE: copy the root value, then verify at `projectsEvaluated`: any drift fails the build naming the
   project, the property, and the root-owned value. No holder cooperation needed (no lock flags, no custom
   setters).

## Decisions proposed (confirm or overrule in review)

| # | Decision | Proposal | If you prefer otherwise |
|---|----------|----------|-------------------------|
| D1 | Ownership | Core-owned central pass. One mechanism, free for every present and future contributor. Settled | Keep contributor-owned copies (revert to previous revision) |
| D2 | Shared types home | New `plugins/canon-api` module (annotations + `CopySemantics` enum, stdlib-only). Core and contributors depend on api; nobody depends on core impl. Settled | Put the annotations in core and accept a core dependency for contributors |
| D3 | Type-driven defaults | Explicit `@Propagate` value always wins. Otherwise the table decides: Gradle `Property<T>` → convention-mapping (`child.convention(root value)` snapshot at copy time; absent root value skips); `List/Set/MapProperty` → DEEP (rejected as not-implemented until its chunk); `Provider` and everything else → throw ("no propagation rule for type …: annotate explicitly or use a supported type"). Private/synthetic/static fields are never considered. The table grows while working. Settled (convention-mapping per maintainer) | Require explicit marks everywhere (drop the table; unannotated stays local) |
| D4 | Immutable enforcement | Verify-at-`projectsEvaluated` with a clear failure (no holder lock state, no custom setters). Settled | Copy+lock flags in holders (previous revision's approach) |
| D5 | Deep semantics | Deferred: enum value stays as specified taxonomy, engine rejects it with a clear not-implemented message until its chunk. No test-double proof, no synthetic collection | Prove it now (previous revision's test-double, or a real collection-valued setting) |
| D6 | Reflection | Plain JDK field reflection only (`getDeclaredField`, `setAccessible`); annotation targets the backing field so contributor code stays clean. No kotlin-reflect. If FIELD-target access proves awkward in implementation, fall back to kotlin-reflect and record why | kotlin-reflect from the start |
| D7 | Docs | Contract gains the annotation + type-table rule; README propagation paragraph rewritten to implemented reality + CHANGELOG entry | Plan only, no README change |

## Acceptance-test layering (uses the Chunk 1 model, no changes)

- **L1 (bulk):** `ProjectBuilder` — call the engine's copy function directly: root `scheme = "custom"` + pristine
  child → child reads `"custom"`; drifted child value fails verification with project + property + root value
  named; `parsedVersion` ignores child-local `version`; a test-double holder proves the table (bare
  `Property<String>` inherits via convention without annotation, override wins; absent root value skips;
  unknown public type throws).
- **L2 (two tests):** `GradleRunner` multi-project — (a) root sets `scheme`, child guards the inherited value in
  its script → green `help`; (b) child sets `scheme` → build fails with the immutable message (`buildAndFail`,
  assert output contains it).
- No L3/L4 in this chunk.

## Acceptance criteria

- [ ] AC1 — Root-set `scheme` is visible in children (L1 direct + L2 inherited).
- [ ] AC2 — A child that sets `scheme` fails the build with a message naming the project, the property, and the
      root-owned value (L1 direct + L2 `buildAndFail`).
- [ ] AC3 — `parsedVersion` in a child always reflects the root project's version, regardless of the child's own
      version.
- [ ] AC4 — Engine lives in core, annotations in `canon-api`; versioning module depends on api only (no core
      classes on its compile classpath); core never names contributor types.
- [ ] AC5 — Type table proven: bare `Property` copies without annotation; unknown public types fail with the
      no-rule message (L1 test-double holder).
- [ ] AC6 — `./gradlew check` green (all suites, all modules); no new warnings; config-cache reuse on the second run.

## Small steps (in order, each verifiable — one step, one commit after review)

- [x] **4.1 `canon-api` module.** New module (group `com.mreil.canon`, Kotlin, stdlib-only): `CopySemantics` enum +
   `@Propagate` annotation (FIELD target, runtime retention, default SHALLOW). Verify: compiles.
2. **4.2 Core copy engine.** Root `afterEvaluate` walk over the `canon` tree: field enumeration (skip
   private/synthetic/static), explicit-annotation-wins, type table per D3, DEEP rejected, immutable drift
   verification at `projectsEvaluated`. JDK reflection only. Verify: compiles (behavior lands in 4.4).
3. **4.3 Contributor adopts.** `scheme` gets explicit `@Propagate(IMMUTABLE)`; versioning's compile dependency
   stays api-only. Verify: compiles.
4. **4.4 L1 proof.** `ProjectBuilder` matrix for AC1–AC5 incl. the table test-double (injected `SoftAssertions`,
   in-suite deps). Verify: `:plugins:canon-core:test` and `:plugins:canon-versioning:test` green.
5. **4.5 L2 proof.** Two `GradleRunner` multi-project tests (inherit-guard green + override-fails).
   Verify: `:plugins:canon-versioning:functionalTest` green.
6. **4.6 Contract + propagation docs.** README contributor section gains the annotation + type-table rule;
   propagation paragraph rewritten to implemented reality (per D7) + CHANGELOG entry. Verify: prose describes
   exactly what the tests prove.
7. **4.7 Full gate.** `./gradlew check --rerun-tasks` green (all suites, all modules); update `plans/README.md`
   (Chunk 4 → done). Verify: every suite executed.
8. **4.8 Chunk 4 retrospective (required before Chunk 5).** Same gate as before, plus: record the final
   answers to review Q2/Q3 as built (convention-mapping behavior, strictness evidence — what threw, what was
   added to the table). Record outcome, amend `AGENTS.md` if needed.

## Verification

- `./gradlew check` exit 0 with every suite in every module executing (AC6/4.7).
- L1 covers AC1–AC5 at unit level; L2 proves the real-build timing.
- Reviewer approval of D1–D7 counts as verification for the mechanism shape.

## Review questions

1. Confirm the remaining open points: D3 (convention-mapping + table + throw-on-unknown), D5 (deep deferred
   with engine rejection), D6 (JDK field reflection, no kotlin-reflect), D7 (docs). D1/D2/D4 already settled.
2. Resolved: `Property → convention-mapping` (parent value becomes child convention, snapshot at copy time;
   absent root value skips).
3. Open, strict by default: engine walks all non-private fields and throws on unknown public types. If
   implementation hits incidental public state that should not propagate, the table gains an explicit
   local-only rule instead of silently skipping. Both Q2 and Q3 must have final answers recorded in the 4.8
   retrospective.
