# 01 — Build scaffolding + test infrastructure

Status: **approved** — ready for implementation.

## Goal

Give every later chunk a place to build and a way to prove itself test-first: an empty root project with a
`libs` version catalog, JVM test suites (JUnit 5 + AssertJ), a `ProjectBuilder` unit baseline, a `GradleRunner`
functional baseline, an agreed fixture convention, an agreed Farley L1–L4 mapping with special care on L2, and a
reserved (not yet built) design for the Chunk 8 linking suite.

## Non-goals (explicitly deferred)

- Any production plugin code (Chunks 2+). Scaffolding may create an empty module skeleton so Chunk 2 can start
  test-first immediately, but no `easy` extension yet.
- Real publishing / release automation. Only the *design* of the local test-publish consumed by Chunk 8.
- Smarter test fixtures. Explicit strings until duplication hurts.
- Any external Gradle plugins (per SPEC). `java-gradle-plugin`, `jvm-test-suite`, and core `maven-publish`
  (if needed for the design sketch) are Gradle built-ins and allowed.

## Decisions (agreed with maintainer)

| # | Decision | Agreed fact |
|---|----------|-------------|
| D1 | Gradle version | Wrapper pinned to **9.8.1** (requires Java 17+, matches host JDK 17.0.21). |
| D2 | Build-script language | **Kotlin DSL everywhere** — repo build scripts and test-fixture builds (no Groovy fixture default). |
| D3 | Module layout | Root stays empty. Chunk 1 creates exactly one empty skeleton module **`plugins/easy-core`** (`java-gradle-plugin` applied, no plugin id registered yet) so Chunk 2 has a home without deciding the final multi-module shape. |
| D4 | Test suites | Each plugin module gets `testing.suites`: `test` (unit, `ProjectBuilder`) + `functionalTest` (GradleRunner, wires `pluginUnderTestMetadata`). Linking suite designed now, built in Chunk 8. |
| D5 | Dependency versions | `libs` catalog: `junit-bom` (managed), `junit-jupiter`, `assertj-core` (3.x). Gradle API via `gradleApi()` / `gradleTestKit()`, not catalog. |

## Acceptance-test layering (Farley L1–L4 for this repo)

- **L1 — unit:** `ProjectBuilder`, no daemon, no script DSL. Carries the bulk of coverage. Example: "applying core registers `easy`".
- **L2 — DSL / contract:** `GradleRunner` with a *minimal build script written as an explicit string* — this is the
  user-facing interaction language. Every L2 test must read as a build-author story ("given this `easy {}` block,
  the build sees …"). Fixture scripts are inline strings at the call site; extract helpers only on the third
  duplication.
- **L3 — integrated:** `GradleRunner` with root + child projects (multi-project `settings` string + two build
  strings). Reserved for propagation/bundling behavior (Chunks 4–6).
- **L4 — linking / system:** separate setup consuming *published* artifacts from a local Maven repo (Chunk 8).
  Designed in this chunk (§ step 1.7), built in Chunk 8. Never runs as part of plain `test`; runs after
  test-publish.

## Fixture convention (binding from here on)

1. No checked-in `src/test/resources/*.gradle(.kts)` fixture files.
2. Each `GradleRunner` test writes its test project with an explicit private helper taking strings — as
   implemented in `GradleRunnerHarnessTest`, `write(fileName, content)` with file contents as `"""` text blocks —
   into `@TempDir`. One private helper per test class until duplication forces a shared one.
3. A new fixture abstraction is introduced only when a concrete duplication appears, with the duplication cited
   in the PR/plan update.

## Acceptance criteria (all must hold; no production code beyond skeleton)

- [x] AC1 — `./gradlew check` is green on the skeleton (compiles, runs `test` + `functionalTest` suites).
- [x] AC2 — `test` suite contains one sample `ProjectBuilder` test proving the harness (e.g. a trivial empty-project
      assertion or the `easy-core` skeleton presence), written JUnit 5 + injected AssertJ `SoftAssertions`.
- [x] AC3 — `functionalTest` suite contains one sample `GradleRunner` test proving the harness: writes
      `settings` + `build` scripts from explicit strings, runs `help` (or equivalent no-op task), asserts success.
- [x] AC4 — `gradle/libs.versions.toml` owns all test dependency versions; no hardcoded versions in module builds.
- [x] AC5 — L1–L4 mapping (§ above) and the fixture convention (§ above) are agreed as written, or amended in
      review — they become binding for Chunks 2+.
- [x] AC6 — Linking-suite design is recorded (§ "Linking design" below): suite name, publish→consume ordering, and
      confirmation it stays out of `check` until Chunk 8. No implementation required.
- [x] AC7 — `README.md` "Roadmap" + `plans/README.md` reflect Chunk 1 scope; `CHANGELOG.md` gets an entry only if a
      user-visible change lands (scaffolding alone: no entry beyond the plan).

## Linking design (reserved for Chunk 8, agreed here)

- A dedicated source set / suite (proposed name: `linkingTest`, or a standalone `linking/` project — final call in
  Chunk 8) that does **not** run in normal `check`.
- Ordering: `test-publish` (all plugin markers + jars via `maven-publish` to `build/local-repo`) → `linkingTest`
  consumes them by coordinates through `pluginManagement { repositories { maven { url = localRepo } } }`.
- Chunk 1 delivers only this paragraph as agreement; no publish config, no linking test yet.

## Small steps (in order, each verifiable — one step, one commit after review)

- [x] **1.1 Wrapper + settings.** Add Gradle 9.8.1 wrapper; `settings.gradle.kts` with `rootProject.name`, empty root
   (no production logic in root). The `libs` catalog needs no `dependencyResolutionManagement` block:
   `gradle/libs.versions.toml` at the default location is auto-imported (an explicit `from(...)` fails with
   "Multiple 'from' invocations"). A minimal empty catalog ships in this step so settings evaluates; real
   versions land in 1.2. Also adds `.gitignore` (`build/`, `.gradle/`, `.idea/`) and `gradle.properties`: `version=0.0.1-SNAPSHOT`,
   `org.gradle.caching=true`, `org.gradle.configuration-cache=true`, `org.gradle.daemon=true`,
   `org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8`, `org.gradle.parallel=true`,
   `org.gradle.tooling.parallel=true` (explicit — relying on the `parallel` default is deprecated since 9.4),
   `org.gradle.vfs.watch=true`, `org.gradle.warning.mode=all`. Deliberately not set: `configureondemand`
   (incubating, unnecessary next to the configuration cache) and `configuration-cache.problems` (default `fail`
   stays — warn mode is a troubleshooting aid, not a persistent setting). Verify: wrapper runs `./gradlew help`;
   `version` resolves to `0.0.1-SNAPSHOT`; second consecutive run reports `Reusing configuration cache`.
- [x] **1.2 Version catalog.** `gradle/libs.versions.toml` with `junit-bom`, `junit-jupiter`, `assertj-core`. Verify:
   catalog resolves (build compiles against it).
- [x] **1.3 Module skeleton + test conventions.** Create `plugins/easy-core` (`java-gradle-plugin`, `jvm-test-suite`);
   JUnit Platform; AssertJ soft assertions available. No plugin id, no extension code. Verify: module compiles.
- [x] **1.4 L1 proof.** One `ProjectBuilder` sample test in `test` (JUnit 5 + injected `SoftAssertions`). Verify: `./gradlew :plugins:easy-core:test` green.
- [x] **1.5 L2 proof.** `functionalTest` suite + `pluginUnderTestMetadata`; one `GradleRunner` sample test with
   inline-string `settings`/`build` files running `help`. Verify: `./gradlew :plugins:easy-core:functionalTest` green.
- [x] **1.6 Conventions doc.** Confirm L1–L4 + fixture rules above (amend in review if needed) and reference them from
   the Chunk 2 plan. Verify: this file's two sections are the final wording.
- [x] **1.7 Linking reservation.** Record the "Linking design" paragraph as the binding sketch; explicitly *not*
   implemented. Verify: `plans/README.md` notes Chunk 8 dependency.
- [x] **1.8 Full gate.** `./gradlew check` green from a clean checkout; update `plans/README.md` (Chunk 1 → done),
   `README.md` roadmap if stale. Finding: `check` did not run `functionalTest` until explicitly wired
   (`tasks.check.dependsOn("functionalTest")` in the module build) — fixed in this step. Verify: gate output
   pasted in the landing report.
- [ ] **1.9 Chunk 1 retrospective (required before Chunk 2).** Pause after the gate is green and review: how did Chunk 1
   go, do our approach / rules / conventions in `AGENTS.md` need changes, and are we happy with the testing model
   (L1–L4 split, fixture convention, suite layout)? Record the outcome (keep / adjust + what changes) in the landing
   report and amend `AGENTS.md` / this plan if needed. Verify: retrospective notes exist and any agreed convention
   changes are applied before Chunk 2 starts.

## Verification

- `./gradlew check` exit 0 from clean checkout (AC1).
- Per-suite runs for AC2/AC3 quoted in the landing report.
- Reviewer approval of L1–L4 + fixture + linking-design wording (AC5/AC6) counts as verification for those items.
- Chunk 1 is not done until the 1.9 retrospective ran and its outcome is recorded.

## Review outcome (resolved)

1. D1–D5 confirmed as recorded in Decisions above (Kotlin DSL everywhere per maintainer).
2. L2-as-DSL-contract framing is the lens for all future `GradleRunner` tests until revisited.
3. Linking suite stays out of `check` until Chunk 8.

