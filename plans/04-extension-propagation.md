# 04 — Root → child extension propagation (core-owned copy engine)

Status: **approved** — D1–D7 settled (D3 with an intentionally incomplete type table,
D5 deep deferred to its chunk, D6 with a kotlin-reflect fallback if needed). Work proceeds in step order.

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
- `versioning.scheme` (`Property<String>`, convention `"semver"`): explicitly `@Propagate(IMMUTABLE)` — project-wide, locked
  by verification. (Per the SPEC rule, public holder state uses Gradle property types; plain state always
  requires an explicit mark to propagate at all.)
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
shape: the annotation targets the backing *field* (so contributors write a plain `@Propagate` with no use-site
target) and the engine uses plain JDK field reflection — no kotlin-reflect (see D6). `resolveMode` lives in one function: an explicit annotation value always wins; an absent annotation
infers from the field, but only if the field is part of the holder's public surface (public field or
public getter — Kotlin backing fields are always private, so visibility is read off the accessors).
Internal and framework state stays local with no annotation needed. Core's copy pass, registered on root
`afterEvaluate`, considers each non-static, non-synthetic field of every `canon` sub-extension in every
project except root:

1. Explicit `@Propagate(...)` wins as written. Otherwise `inferFromType` decides: Gradle `Property<T>` →
   SHALLOW; `List/Set/MapProperty` → DEEP; anything else → throw ("no propagation rule for type …:
   annotate explicitly"). SHALLOW executes per type: `Property<T>` → `child.convention(root value)`
   snapshot (skipped when the root value is absent); plain mutable fields → direct write.
2. DEEP: rejected with "not implemented yet" until its chunk (see D5).
3. IMMUTABLE: same writers as SHALLOW, then verify at `projectsEvaluated`: any drift fails the build naming the
   project, the property, and the root-owned value. No holder cooperation needed (no lock flags, no custom
   setters).

## Decisions proposed (confirm or overrule in review)

| # | Decision | Proposal | If you prefer otherwise |
|---|----------|----------|-------------------------|
| D1 | Ownership | Core-owned central pass. One mechanism, free for every present and future contributor. Settled | Keep contributor-owned copies (revert to previous revision) |
| D2 | Shared types home | New `plugins/canon-api` module (annotations + `CopySemantics` enum, stdlib-only). Core and contributors depend on api; nobody depends on core impl. Settled | Put the annotations in core and accept a core dependency for contributors |
| D3 | Type-driven defaults | Explicit `@Propagate` value always wins. Otherwise participation requires a public surface — public field or public getter (Kotlin backing fields are always private, so visibility is read off the accessors): internal state stays local with no annotation needed. Given participation, absent annotation → `inferFromType`: `Property<T>` → SHALLOW (convention-mapping snapshot; absent root value skips); `List/Set/MapProperty` → DEEP (rejected as not-implemented until its chunk); anything else → throw ("no propagation rule"). Approved: the table stays intentionally incomplete —
   per-type defaults are added as concrete needs appear, never speculatively. | Walk all fields and maintain an ignore-list of infra types (rejected: whack-a-mole, proven wrong by `_gr_map_`/`_gr_mc_`) |
| D4 | Immutable enforcement | Verify-at-`projectsEvaluated` with a clear failure (no holder lock state, no custom setters). Settled | Copy+lock flags in holders (previous revision's approach) |
| D5 | Deep semantics | Deferred: enum value stays as specified taxonomy, engine rejects it with a clear not-implemented message until its chunk. No test-double proof, no synthetic collection. Approved:
   deep stays deferred | Prove it now (previous revision's test-double, or a real collection-valued setting) |
| D6 | Reflection | Plain JDK field reflection only (`getDeclaredField`, `setAccessible`); annotation targets the backing field so contributor code stays clean. No kotlin-reflect. If FIELD-target access proves awkward in implementation, fall back to kotlin-reflect and record why. Approved with that fallback | kotlin-reflect from the start |
| D7 | Docs | Contract gains the annotation + type-table rule; README propagation paragraph rewritten to implemented reality + CHANGELOG entry. Approved | Plan only, no README change |

## Acceptance-test layering (uses the Chunk 1 model, no changes)

- **L1 (bulk):** `ProjectBuilder` — call the engine's copy function directly: root `scheme = "custom"` + pristine
  child → child reads `"custom"`; drifted child value fails verification with project + property + root value
  named; `parsedVersion` ignores child-local `version`; a test-double holder proves the pass mechanics
  (unannotated public `Property` inherits via convention, override wins, absent root value skips; unannotated
  public unknown-typed field throws; private and framework fields are ignored; explicit modes still win).
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
- [ ] AC5 — Mechanics proven on a test-double holder: unannotated public `Property` inherits via convention
      (override wins, absent root value skips); unannotated public unknown-typed field throws; private and
      framework fields are ignored; explicit modes still win.
- [ ] AC6 — `./gradlew check` green (all suites, all modules); no new warnings; config-cache reuse on the second run.

## Small steps (in order, each verifiable — one step, one commit after review)

- [x] **4.1 `canon-api` module.** New module (group `com.mreil.canon`, Kotlin, stdlib-only): `CopySemantics` enum +
   `@Propagate` annotation (FIELD target, runtime retention, default SHALLOW). Verify: compiles.
- [x] **4.2 Core copy engine.** Root `afterEvaluate` walk over the `canon` tree: per-field `resolveMode`
   (explicit wins, absent → participation gate on public accessors, then type table); SHALLOW writers per
   field type (convention for `Property`, direct write for plain mutable), DEEP rejected, immutable drift
   verification at `projectsEvaluated`. JDK reflection only, no kotlin-reflect. Public entry points
   (L1 calls them directly). As built: `object CanonPropagation` + top-level `RecordedCopy`, KDoc on
   the object, early L1 matrix in `CanonPropagationTest` (mechanics only; versioning specifics land
   in 4.4). Verify: compiles (behavior lands in 4.4).
- [x] **4.3 Contributor adopts.** `scheme` gets explicit `@Propagate(IMMUTABLE)`; versioning's compile dependency
   stays api-only. As built: plain `@Propagate` with no use-site target compiles on a Kotlin `var`
   (annotation lands on the backing field). Verify: compiles.
- [x] **4.4 L1 proof.** `ProjectBuilder` matrix for AC1–AC5 incl. the table test-double (injected `SoftAssertions`,
   in-suite deps). As built: engine matrix in `CanonPropagationTest` plus `VersioningPropagationTest`
  (inherited scheme, drift record, `parsedVersion` vs child version); the L1 run caught and fixed two
  engine bugs (decorated-subclass hierarchy walk, internal-visibility gate). Verify: `:plugins:canon-core:test` and `:plugins:canon-versioning:test` green.
- [x] **4.5 L2 proof.** Two `GradleRunner` multi-project tests (inherit-guard green + override-fails).
   Verify: `:plugins:canon-versioning:functionalTest` green.
- [x] **4.6 Contract + propagation docs.** README contributor section gains the annotation + type-table rule;
   propagation paragraph rewritten to implemented reality (per D7) + CHANGELOG entry. Verify: prose describes
   exactly what the tests prove.
- [x] **4.7 Full gate.** `./gradlew check --rerun-tasks` green (all suites, all modules); update `plans/README.md`
   (Chunk 4 → done). Verify: every suite executed.
8. **4.8 Chunk 4 retrospective (required before Chunk 5).** Same gate as before, plus: record the final
   answers to review Q2/Q3 as built (convention-mapping behavior, strictness evidence — what threw, what was
   added to the table). Record outcome, amend `AGENTS.md` if needed.

## Verification

- `./gradlew check` exit 0 with every suite in every module executing (AC6/4.7).
- L1 covers AC1–AC5 at unit level; L2 proves the real-build timing.
- Reviewer approval of D1–D7 counts as verification for the mechanism shape.

## Review questions

1. Settled: D3 (absent→infer on public surface, explicit wins, unknown throws; table grows on demand),
   D5 (deep deferred with engine rejection), D6 (JDK field reflection, kotlin-reflect fallback allowed),
   D7 (docs). D1/D2/D4 already settled.
2. Resolved: `Property → convention-mapping` (parent value becomes child convention, snapshot at copy time;
   absent root value skips).
3. Resolved: strictness lives in three places — inference throws on unknown types, explicit modes are
   honored as written, and immutable drift fails the build. Non-public state stays local via the accessor
gate (no ignore-list needed — proven by `_gr_map_`/`_gr_mc_` framework fields). If implementation hits a
   case the table mishandles, extend the table rather than silently skipping; record the evidence in the
   4.8 retrospective.
