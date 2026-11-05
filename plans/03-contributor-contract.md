# 03 — Contributor plugin contract (`canon.versioning` reference)

Status: **approved** — ready for implementation.

## Goal

Prove the extension point with the first real consumer: a `canon.versioning` contributor that manages versioning
schemes — defaulting to semver, parsed from the root project's version — plus the written contract every future
contributor follows. This contributor is also the designated candidate for testing the external build-service API
in Chunk 7, so its version-resolution logic must stay dependency-light and liftable (constraint, not code, in
this chunk).

## Non-goals (explicitly deferred)

- Additional versioning schemes (only semver exists in this chunk; the scheme point is shaped but single-valued).
- Version bumping, writing, or git-derived versions.
- The shared build service itself (Chunk 7) — no service code here.
- Root → child value propagation (Chunk 4). Reading `rootProject.version` is plain project API, not propagation;
  every project still gets a fresh `versioning` holder.

## The contract (what a contributor must do)

1. Coordinates live under `com.mreil.canon.*`; the sub-extension name (`versioning`) is the suffix of the plugin id.
2. The contributor is applied to the **root** project and requires `canon` to be present — put there by the build
   author or the master bundle (Chunk 5) — failing fast with a clear message otherwise. It never applies core
   itself and needs no core classes on its classpath (Gradle API only), so bundled and third-party contributors
   are the same code shape; "bundled" is release coordination, not a second contract.
3. It registers its holder via `canon.extensions.create("<name>", <Type>.class)` on every project the core
   wiring covers (i.e. just do it per project the same way core does; no new core API).
4. Names are unique per `canon` tree: registering an already-taken name fails fast with a clear message instead
   of Gradle's generic duplicate-extension error.
5. Re-applying the contributor is harmless (same `findByName` idempotency guard as core).

No new core framework code: the contract is a documented pattern plus the reference implementation. If a second
contributor later shows the pattern needs machinery, that is Chunk 6's problem. The `canon-api` split happens
when the first genuinely shared type appears (Chunk 4 propagation semantics or Chunk 7 service interfaces,
whichever comes first) — not before.

## Decisions (agreed with maintainer)

| No. | Decision            | Proposal                                                                                                                                                                                                                                                                                                                                                                           | If you prefer otherwise                                                       |
|-----|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| D1  | Module layout       | New module `plugins/canon-versioning` (group `com.mreil.canon`, id `com.mreil.canon.versioning`, package `com.mreil.canon.versioning`, **no** dependency on the core module), and rename `plugins/easy-core` → `plugins/canon-core` in the same step (one `git mv`, history preserved) so the layout stops lying about the product name                                            | Settled                    |
| D2  | Holder type         | Plain open class `VersioningExtension(project)`, mirroring `CanonExtension`; `@Inject ObjectFactory` only when nested managed objects are needed                                                                                                                                                                                                                                   | —                                                                             |
| D3  | Ordering            | Contributor requires `canon` present (`findByName` null → `IllegalStateException` naming the missing core id and the bundle); never applies core, needs no core classes. Ordering is the author's/bundle's job (Chunk 5 owns it for real). Settled per maintainer                                                                                                                  | —                                                                             |
| D4  | Collision           | Explicit `findByName` pre-check throwing `IllegalStateException("canon already has a '<name>' contributor — two contributors claim the same name")`; same holder type re-applied resolves to the existing instance (Kotlin `check(existing is VersioningExtension) { ... }`); anything else under the same name fails. Settled                                                                                                                                                          | Settled              |
| D5  | Semver library      | `org.semver4j:semver4j:6.0.0` (actively maintained, June 2025). Confirmed by maintainer                                                                                                                                                                                                                                                                                            | —                                                                             |
| D6  | Extension API shape | `VersioningExtension` exposes `scheme` (default `"semver"`, the single supported value for now) and `parsedVersion()` derived from `rootProject.version` at configuration time; unparseable versions fail fast with a clear message naming the project and value. Pure parsing lives in a dependency-light internal function so Chunk 7 can lift it into a build service untouched | Settled |
| D7  | Contract home       | Short "Writing a contributor plugin" section in `README.md` (the five rules above) + this plan as the normative reference                                                                                                                                                                                                                                                          | Settled                                                   |

## Acceptance-test layering (uses the Chunk 1 model, no changes)

- **L1 (bulk):** `ProjectBuilder` — core + contributor yields `canon` + `canon.versioning` on root and child;
  contributor-only fails fast with the D3 message and registers nothing; re-apply harmless (same instance);
  second contributor with the same name fails with the D4 message; `scheme` defaults to `"semver"`; root version
  `"1.2.3"` and `"1.2.3-SNAPSHOT"` parse; garbage (`"banana"`) fails with a clear message.
- **L2 (one test):** `GradleRunner` — root applies `com.mreil.canon.core` and `com.mreil.canon.versioning` by id
  with a project version set, child configures `canon { versioning {} }`, `help` succeeds. Proves id resolution
  and real-build wiring end to end (version semantics stay L1).
- No L3/L4 in this chunk.

## Acceptance criteria

- [x] AC1 — Applying core + contributor to the root registers `canon` and `canon.versioning` on root and every
      child.
- [x] AC2 — Applying only the contributor fails fast with the require-core message and registers nothing.
- [x] AC3 — Re-applying the contributor keeps the existing `versioning` instance (no replace, no error).
- [x] AC4 — A different contributor claiming `versioning` fails fast with the D4 message.
- [x] AC5 — `scheme` defaults to `"semver"`; the parsed version equals the root project's version for valid
      semver (including `-SNAPSHOT`); anything else fails with a message naming the project and the value.
- [x] AC6 — `./gradlew check` green (all suites, all modules); no new warnings; config-cache reuse on the second run.

## Small steps (in order, each verifiable — one step, one commit after review)

- [x] **3.1 Module + coordinates (+ rename).** Create `plugins/canon-versioning` (`java-gradle-plugin`,
   `jvm-test-suite`, Kotlin, in-suite deps, group `com.mreil.canon`, id `com.mreil.canon.versioning`, no core
   dependency); semver library in the catalog (per D5); rename `plugins/easy-core` → `plugins/canon-core`
   including the settings include (per D1). Verify: all modules compile, `./gradlew check` green.
- [x] **3.2 Contributor logic.** `VersioningExtension` + `VersioningPlugin` implementing the contract (D3
   require-present, D4 collision guard, idempotency) and the D6 API shape; pure parsing function kept
   dependency-light for Chunk 7. Verify: compiles.
- [x] **3.3 L1 proof.** `ProjectBuilder` matrix for AC1–AC5 (injected `SoftAssertions`, in-suite deps).
   The suite — but not the shipped artifact — depends on the core module to set up the `canon` precondition.
   Verify: `:plugins:canon-versioning:test` green.
- [ ] **3.4 L2 proof.** `GradleRunner` test per the layering section (inline Kotlin-DSL strings, private `write`
   helper with parent-dir creation). Verify: `:plugins:canon-versioning:functionalTest` green.
- [x] **3.5 Contract doc.** README "Writing a contributor plugin" section (per D7) + CHANGELOG entry under
   Unreleased (`canon.versioning` reference contributor). Verify: prose describes exactly what the tests prove.
- [x] **3.6 JaCoCo test coverage.** Apply the core `jacoco` plugin (built-in, no exception needed) in both
   modules; tool version `0.8.16` from the catalog; per-module XML + HTML reports, no coverage gates yet
   (no basis for thresholds — deciding numbers now would be fiction). Applies repo-wide to present and future
   modules. Verify: `test` + `functionalTest` produce reports, `./gradlew check` green.
- [x] **3.7 Kotlin everywhere + Spotless.** Convert the remaining Java test sources to Kotlin (Java has no
   place in this codebase); Spotless covers Kotlin only. Apply external `com.diffplug.spotless` `8.11.0` via
   the catalog (explicitly allowed by this step, per the AGENTS.md rule) with `ktfmt` google style plus
   4-space block/continuation indent (current ktfmt offers google vs kotlinlang styles — the old Meta style
   is gone; this matches the 4-space sources and minimizes the first reformatting diff); run `spotlessApply`
   and review the reformatting
   diff as part of the step. `spotlessCheck` rides with `check`. Applies repo-wide to present and future
   modules. Verify: no `src/*/java` dirs remain, `spotlessCheck` green, reformatting reviewed.
- [x] **3.8 Detekt static analysis.** Apply external `io.gitlab.arturbosch.detekt` `1.23.8` via the catalog
   (explicitly allowed by this step, per the AGENTS.md rule); default rule set to start, tune only on concrete
   noise — no custom rule shopping. `detekt` rides with `check`. Applies repo-wide to present and future
   modules. Verify: `detekt` green, findings reviewed.
- [x] **3.9 Full gate.** `./gradlew check --rerun-tasks` green (all suites, all modules; coverage, spotless, and
   detekt included); update `plans/README.md` (Chunk 3 → done). Verify: every suite executed.
- [ ] **3.10 Chunk 3 retrospective (required before Chunk 4).** Same gate as 1.9/2.7: how did Chunk 3 go,
   approach/rules/conventions changes needed, happy with the testing model? Record outcome, amend `AGENTS.md`
   if needed.

## Verification

- `./gradlew check` exit 0 with every suite in every module executing, coverage reports produced, `spotlessCheck` and `detekt` green (AC6/3.9).
- L1 matrix covers AC1–AC5; L2 covers id resolution + real-build wiring.
- Reviewer approval of D1–D7 counts as verification for the contract shape.

## Review outcome (resolved, plus Q3 elaboration)

1. D1–D7 all confirmed.
2. Resolved: distinguish by implementation class — same holder type re-applied resolves to the existing instance,
   anything else under the same name fails (Kotlin `check(existing is VersioningExtension) { ... }`).
3. Resolved: option (a) — plain dependency-light function, L1-tested; Chunk 7 lifts it then. No speculative
   interface.
