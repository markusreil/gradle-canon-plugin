# 00 — High-level plan: gradle-easy-plugin-2

Status: **done (roadmap)** — awaiting maintainer review. Detailed `01–08` plans will be written one chunk at a time after approval.

## Goal (from SPEC.md)

A convention-plugin system for Gradle: a core `easy` extension container auto-registered on all projects,
contributor plugins adding sub-extensions (e.g. `easy.jvm`), a master plugin bundling core + chosen contributors
via classpath auto-apply, third-party contributors wiring in alongside master, root→child extension-tree copying
with deep/shallow/immutable semantics, and shared build services as separate artifacts.

## Build order (each chunk is one reviewed deliverable)

### Chunk 1 — Build scaffolding + test infrastructure (first, unlocks everything)

*Why first:* every later chunk needs a place to build and a way to prove itself test-first.
*Contents:*
- Empty root project, `settings.gradle`, `libs` version catalog.
- JVM test suites, JUnit 5 + AssertJ soft assertions, no external plugins unless explicitly allowed.
- `ProjectBuilder` unit-test baseline (the default test layer).
- `GradleRunner` functional-test baseline; decide the fixture convention: test-project files created from
  explicit strings at test runtime, smarter fixtures only when a concrete need appears.
- Dave Farley's four-layer acceptance model: agree what L1–L4 mean here, with special care on L2 (the
  user-facing build-script DSL the tests speak).
- Design (not necessarily fully build) the "linking" suite: a separate test setup that test-publishes to a
  local Maven repo and then consumes the plugins as published artifacts (Chunk 8 builds on this).
*Exit criteria:* `./gradlew check` green on an empty codebase skeleton; one sample `ProjectBuilder` test and one
sample `GradleRunner` test prove both harnesses; L1–L4 + fixture conventions written down in the Chunk 1 plan.

### Chunk 2 — Core `easy` extension plugin

*Why second:* the anchor everything attaches to.
*Contents:* `EasyPlugin` (or equivalent) applied to root + auto-registered on all projects (`allprojects` /
`subprojects` wiring or Gradle's plugin-application semantics — detailed plan decides); the `easy` named-container
extension that contributors can later extend. Unit-tested with `ProjectBuilder`: applied once, `easy` exists on
root and children, idempotent re-application.
*Exit criteria:* `easy {}` resolves in a test build on root and on a child project.

### Chunk 3 — Contributor plugin contract (`easy.jvm` as reference contributor)

*Why third:* proves the extension point with exactly one real consumer before propagation complicates it.
*Contents:* SPI/contract for contributors to register a sub-extension under `easy` (e.g. `easy.jvm`); one minimal
reference contributor (JVM conventions placeholder — real convention logic comes later, the wiring is the point).
Covers ordering (contributor applied before/after core), naming collisions, and idempotency.
*Exit criteria:* applying core + reference contributor yields `easy.jvm` on all projects via `ProjectBuilder`
tests; contract documented for future contributors.

### Chunk 4 — Root → child extension-tree propagation (deep / shallow / immutable)

*Why fourth:* needs a tree with at least two levels (core + one contributor) to be meaningful; deferring it keeps
Chunks 2–3 simple (children just get fresh defaults).
*Contents:* copy semantics per value type — deep (extendable collections merge), shallow (child overwrites),
immutable (locked after root evaluation); when copying happens (after root evaluation / before child execution);
what "override in child" means in the DSL. This is the subtlest behavior — expect the most test cases here.
*Exit criteria:* matrix of propagation tests (each semantic × override/no-override) green at `ProjectBuilder`
level, plus one `GradleRunner` DSL-level test proving the L2 experience.

### Chunk 5 — Master bundling plugin (classpath auto-apply)

*Why fifth:* bundling is trivial once core + one contributor + propagation exist; doing it earlier would freeze the
bundle shape prematurely.
*Contents:* standalone `easy-master` plugin that brings core + the curated contributor set; contributors
auto-applied purely by being on the master plugin's classpath (detailed plan evaluates `pluginManager.apply` in
master vs. marker/aggregator artifacts). Versioning story for the bundle.
*Exit criteria:* applying only the master plugin in a test build yields core + bundled contributors with no extra
`apply` statements; `ProjectBuilder` + one `GradleRunner` test.

### Chunk 6 — Third-party contributor interop

*Why sixth:* can only be proven against a finished master bundle — "applies alongside master and wires in
correctly" needs master to exist.
*Contents:* a sample out-of-tree contributor applied in the build file next to master; ordering with master,
late-registration into the already-propagated tree, conflict behavior when names collide with bundled
contributors.
*Exit criteria:* master + third-party contributor coexist in one test build; tree contains both namespaces;
collision produces a clear, tested error.

### Chunk 7 — Shared build-service APIs as separate artifacts

*Why seventh:* services are the cross-contributor communication layer; building them after two real contributors
exist (bundled + third-party) means the API is shaped by actual consumers, not guesses.
*Contents:* one sample shared build service in its own artifact/module (e.g. a service owned by the `jvm`
contributor and consumed by another); registration, lifecycle, and isolation rules; "must be separate build
artifacts" enforced by the build layout.
*Exit criteria:* contributor B consumes contributor A's service in a test build without depending on the whole
contributor; service lifecycle covered by tests.

### Chunk 8 — Published-artifact linking verification (last)

*Why last:* end-to-end proof that the whole system works as consumers will see it — via versioned artifacts, not
project dependencies. Runs on the Chunk 1 test-publish infrastructure.
*Contents:* separate verification project/suite: test-publish all artifacts to a local Maven repo, then apply
master + third-party contributors by coordinates in a fresh build and assert the full `easy` tree links and
configures correctly.
*Exit criteria:* linking suite green in CI order (publish → consume); documents the release path.

## What this ordering defers (deliberately)

- Real convention logic inside contributors (e.g. what `easy.jvm` actually configures) — wiring first, opinions later.
- Smarter test fixtures — explicit strings until duplication hurts.
- Publishing/release automation — only the local test-publish needed for Chunk 8; real publishing is a follow-up.

## Review questions for the maintainer

1. Is one reference contributor (`easy.jvm`) enough for Chunks 3–4, or do you want two to prove multi-contributor propagation earlier?
2. Should Chunk 7's sample service be owned by `easy.jvm`, or do you have a concrete cross-cutting service in mind?
3. Does the Chunk 8 linking suite need to run in the same `./gradlew check`, or is a separate CI job acceptable?
