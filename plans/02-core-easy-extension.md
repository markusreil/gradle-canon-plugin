# 02 — Core `canon` extension plugin

Status: **approved** — ready for implementation.

## Goal

The anchor everything attaches to: a core plugin that, applied to the root project, registers a `canon`
extension container on **all** projects (root + current and future children). The container is empty in this
chunk — contributors (Chunk 3) and propagation (Chunk 4) come later. Applying the plugin twice must be harmless.

## Non-goals (explicitly deferred)

- Any contributor sub-extension (`canon.jvm` is Chunk 3).
- Root → child value propagation (Chunk 4). In this chunk every project gets a fresh, empty `canon`.
- Master bundling (Chunk 5), third-party interop (Chunk 6), build services (Chunk 7).
- Real convention logic inside `canon` — no properties yet, just the extensible holder.

## Decisions (agreed with maintainer)

| # | Decision | Proposal | If you prefer otherwise |
|---|----------|----------|-------------------------|
| D1 | Plugin coordinates | Group `com.mreil.canon`, master id `com.mreil.canon` (Chunk 5), core id `com.mreil.canon.core`, implementation class `CanonPlugin`, extension type `CanonExtension`, Java package `com.mreil.canon.core` (test package `easy.core` renamed to match in 2.1) | Settled |
| D2 | Extension holder type | Plain class `CanonExtension` created via `extensions.create("canon", …)` — every such object is `ExtensionAware`, so contributors can later call `canon.extensions.create("jvm", …)` with no further core work | Deferred: abstract `@Inject ObjectFactory` style only when nested managed objects are needed |
| D3 | All-projects wiring | Plugin applied to the **root** project registers `canon` on root plus `rootProject.allprojects(…)` (live collection: covers current and later-added children). Applied directly to a non-root project, it registers `canon` on that project only. Duplicate registration guarded by `findByName` → idempotent | `subprojects` + explicit root handling, or require applying to every project |
| D4 | Configuration avoidance | Eager `extensions.create` on all projects (fine for an empty holder; revisit if holder construction ever gets expensive) | Lazy registration — say so and the plan gains a step |

## Acceptance-test layering (uses the Chunk 1 model, no changes)

- **L1 (bulk):** `ProjectBuilder` — root + child (`withParent`), assert `canon` present on both, re-application
  harmless, non-root direct application registers locally only.
- **L2 (one test):** `GradleRunner` with inline Kotlin-DSL scripts — root applies core plugin by id, child writes an
  (empty) `canon {}` block, `help` succeeds. Proves the DSL resolves end to end via the published plugin id
  (through `pluginUnderTestMetadata`, not project dependencies).
- No L3/L4 in this chunk.

## Acceptance criteria

- [x] AC1 — Applying the core plugin to the root project registers a `canon` extension on root and on every child.
      Truly late-added children rely on Gradle's live `allprojects` (a framework guarantee `ProjectBuilder`
      cannot simulate — verified: a `withParent` child built after apply does not re-fire it); the real-build
      path is proven by the multi-project L2 test instead.
- [x] AC2 — Re-applying the plugin to any project does not fail and does not replace the existing `canon` instance.
- [x] AC3 — Applying the plugin directly to a single (non-root) project registers `canon` there without touching
      siblings or the parent.
- [x] AC4 — The plugin is published under its id (`gradlePlugin { plugins { … } }` block, no implementation-class
      application in tests except where the L1/L2 split requires it); the L2 test applies it by id.
- [x] AC5 — `./gradlew check` green (both suites); no new warnings; config-cache reuse on the second run.

## Small steps (in order, each verifiable — one step, one commit after review)

1. **2.1 Coordinates + canon rename (implementation and tests only).** Set group `com.mreil.canon`, plugin id
   `com.mreil.canon.core`, and Java package `com.mreil.canon.core` (renaming test package `easy.core`).
   Committed Chunk 0/1 docs stay untouched. Register the id via `gradlePlugin { plugins { … } }` with no
   implementation yet — or with the stub from 2.2, whichever the implementer prefers. Verify: the plugin descriptor
   `META-INF/gradle-plugins/com.mreil.canon.core.properties` maps the id to the implementation class.
2. **2.2 Holder.** Add `CanonExtension` (empty, extensible holder per D2). No wiring yet. Verify: compiles.
3. **2.3 Apply logic.** `CanonPlugin` implements D3 (root → allprojects live wiring, local-only otherwise,
   `findByName` idempotency guard). Verify: compiles.
4. **2.4 L1 proof.** `ProjectBuilder` tests: root+child presence, idempotent re-apply, local-only
   direct application (injected `SoftAssertions`, in-suite deps). No late-child L1 test: `ProjectBuilder`
   bypasses Gradle's live-`allprojects` firing (proven by a failing attempt); that path is covered by the
   multi-project L2 test. Verify: `:plugins:easy-core:test` green.
5. **2.5 L2 proof.** `GradleRunner` test: root applies plugin by id, child configures empty `canon {}` block (inline
   Kotlin-DSL strings, private `write` helper), `help` succeeds. Verify: `:plugins:easy-core:functionalTest`
   green.
- [x] **2.6 Full gate.** `./gradlew check` green from the working tree; update `plans/README.md` (Chunk 2 → done).
   Verified with `--rerun-tasks`: `test` and `functionalTest` both executed, BUILD SUCCESSFUL.
- [x] **2.7 Chunk 2 retrospective (required before Chunk 3).** Outcome: nothing came up — keep everything as is.
   The `ProjectBuilder`-cannot-simulate-live-`allprojects` limitation (2.4) stands as the known testing-model caveat.

## Verification

- `./gradlew check` exit 0, with `test` + `functionalTest` both executing (AC5/2.6).
- L1 matrix covers AC1–AC3; L2 covers AC4 end to end.
- Reviewer approval of D1–D4 counts as verification for the shape of the API.

## Review outcome (resolved)

1. D1–D4 confirmed (canon coordinates, plain holder with `ObjectFactory` deferred until needed, root +
   live-allprojects wiring, eager creation so the extension is available as early as possible).
2. Rename sweep is implementation + tests only; committed docs stay untouched.
3. Taken as agreed: one L2 test (child `canon {}` block). Say the word to also cover the in-root case.
