# AGENTS.md — gradle-easy-plugin-2

How we work in this repo. Source of truth for workflow; SPEC.md is source of truth for product intent.

## Product in one paragraph

A convention-plugin system for Gradle: one core plugin registers an `easy` extension container on all projects,
contributor plugins add sub-extensions (e.g. `easy.jvm`), a master plugin bundles core + a chosen set of
contributors, third-party contributors can be applied alongside master, the extension tree propagates root → children
with per-value copy semantics (deep / shallow / immutable), and contributors can expose public APIs as shared build
services in separate build artifacts. See `SPEC.md` and `plans/00-high-level-plan.md`.

## How we work

1. **Plan before code.** Every feature starts with a short implementation plan in `plans/`. Prefer small
   deliverables over one huge change. Do not implement a chunk without its plan being reviewed.
2. **Spec tests first.** No implementation without a covering test. Write the failing test, then the minimal
   implementation, then refactor.
3. **Small steps, one chunk at a time.** Implement the plan's chunks in order. Finish + verify one before starting
   the next. Each numbered small step lands as a single git commit, committed after review once that step is
   considered done — one step, one commit.
4. **Acceptance-test layering.** For anything above unit level we follow Dave Farley's four-layer model. Think
   carefully about L2 (the DSL / user-facing interaction language) — it drives how test-project fixtures are
   written. Test-project files consumed by GradleRunner are created from explicit strings at test runtime; introduce
   smarter fixtures only when a concrete need/opportunity appears.
5. **Test pyramid for this repo.**
   - Unit tests with `ProjectBuilder` are the default and carry the bulk of coverage.
   - `GradleRunner` functional tests cover higher-level interaction only.
   - Top-level linking tests (plugin behaves as if published to a Maven repo) live in a separate setup that runs
     after a test-publish to a local repo — see plan Chunk 1 for the dedicated suite design.
6. **Gradle project conventions.**
   - Empty root project; logic lives in subprojects / `plugins/` as designed per plan.
   - `libs` version catalog for all dependencies.
   - Never use top-level `test*` configurations (`testImplementation`, `testRuntimeOnly`, …). Declare test
     dependencies inside the owning `JvmTestSuite` (`testing { suites { named<JvmTestSuite>(…) { dependencies { … } } } }`).
   - JVM test suites; JUnit 5 + AssertJ soft assertions via the injected `SoftAssertions` extension
     (`@ExtendWith(SoftAssertionsExtension.class)`), not the static `assertSoftly` entry point.
   - Production and test sources are Kotlin (`src/main/kotlin`, `src/test/kotlin`, …); the Kotlin version is
     managed in the `libs` catalog. No Java sources anywhere.
   - No external Gradle plugins unless explicitly allowed in the plan.
   - Test suites may depend on sibling modules to set up fixtures (e.g. applying another plugin under test);
     shipped artifacts stay decoupled — the dependency lives in the `testing { suites { … } }` block, never in
     main.
7. **Docs are part of done.** Keep these updated in the same change that makes them stale:
   - `README.md` — consumer view: how a build author uses the plugin (installation, `easy {}` DSL, contributors,
     master plugin, third-party contributors, build services). Never documents internals as if they were the API.
   - `CHANGELOG.md` — every user-visible change gets an entry under `Unreleased` (Keep a Changelog format).
   - `plans/` — each chunk has `plans/NN-<slug>.md` with status (`todo` / `in progress` / `done`); the tracker table
     in `plans/README.md` mirrors the current state. Update both when a chunk starts and when it lands.
8. **Minimal diffs.** Don't refactor unrelated code. Don't add abstractions for hypothetical needs.
9. **Verification before reporting done.** Diagnostics on changed files, related tests green, and a real-surface check
   for anything user-visible. A green suite alone never proves consumer-facing behavior works.

## Repo layout (evolving)

- `SPEC.md` — product spec.
- `plans/` — `README.md` (tracker) + `00-high-level-plan.md` + `NN-<chunk>.md` detailed plans as we write them.
- `README.md` — consumer documentation.
- `CHANGELOG.md` — release history.
- Build sources (to be created in Chunk 1): `settings.gradle`, version catalog, subprojects.

## Plan lifecycle

1. Draft `plans/NN-<slug>.md` with goal, acceptance criteria (incl. which test layers), and small steps.
2. Review with maintainer (explicit approval before implementation).
3. Implement test-first, keeping `README.md` / `CHANGELOG.md` / tracker current.
4. Mark chunk `done` only when its acceptance criteria + verification are met.
