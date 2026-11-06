# 08 — Published-artifact linking verification (last)

Status: **approved** — D1–D4 settled. Work proceeds in step order.

## Goal

Prove the whole system works as consumers will see it: test-publish every artifact to a local Maven
repo, then apply master + third-party contributor by coordinates in a fresh build and assert the
full `canon` tree links and configures. This documents the release path; real publishing stays a
follow-up.

## Non-goals (explicitly deferred)

- Real publishing / release automation (follow-up, per the high-level plan).
- The master fat-jar question (parked in "Later" — linking proves coordinate resolution, which holds
  for either packaging).
- New contributor or service functionality.

## Carried in (settled inputs, not open questions)

Chunks 1–7 settled everything this chunk builds on, including the Chunk 1 linking reservation:
a dedicated suite that stays out of normal `check`, ordering test-publish → consume by coordinates
through `pluginManagement { repositories { maven { … } } }`. Anything outside those contracts fails
fast (AGENTS.md rule 9). DEEP merge and the service consumer pattern stay parked.

## Design

New `linking/` module with a single `linkingTest` JvmTestSuite (JUnit 5 + TestKit, no `main`
sources). It is deliberately *not* wired into `check` — a dedicated task run explicitly (and in CI
after check), keeping `check` offline-friendly per the Chunk 1 agreement.

- **Test-publish.** Every plugin module applies `maven-publish` and configures its publications
  (`mavenJava` for library modules, plugin markers come free with `java-gradle-plugin`). The test
  repo is declared only when `-PtestRepoUrl=…` is passed on the CLI — regular `publish` never sees
  a test URL. One version (current `project.version`, lockstep today). `linkingTest` depends on every
  module's `publishAllPublicationsToTestLocalRepository`, which is the aggregate ordering (no
  separate `testPublish` task).
- **Consume by coordinates.** Fixture builds declare the test repo in `pluginManagement` and apply
  `com.mreil.canon` + `com.example.canon.extra` by id *and version* (current version and repo URL
  passed in as system properties, never hardcoded). Tests assert `canon.versioning` + `canon.extra` resolve, root-set values
  propagate, and an immutable override still fails — the L2 shape, but against published artifacts.
- **All seven modules publish**, including the third-party sample (test-publish is test infrastructure,
  not a publishing story — Chunk 6's constraint stands). The sample keeps its `com.example` id: the
  reserved namespace cannot collide with a live artifact.
- **Sample group:** keep `com.mreil.canon` for all artifacts in test-publish; real third parties use
  their own coordinates, which this suite does not need to prove.

## Decisions proposed (confirm or overrule in review)

| # | Decision | Proposal | If you prefer otherwise |
|---|----------|----------|-------------------------|
| D1 | Suite home | New `linking/` module, `linkingTest` suite only, out of `check`. Settled | Suite inside an existing module |
| D2 | Publish mechanism | `maven-publish` applied in each module, publications configured there; the test repo exists only when `-PtestRepoUrl=…` is passed (regular `publish` never sees it). Settled | `mavenLocal` (`~/.m2`, shared state) |
| D3 | Scope | All modules incl. sample markers; current `project.version` passed to fixtures as a system property, `linkingTest` depends on every publish task. Settled | Subset of modules |
| D4 | Check membership | Separate task, CI-after-check; `check` stays green offline. Settled | Wire into `check` |

## Acceptance-test layering (uses the Chunk 1 model, no changes)

- **L4 (linking):** `linkingTest` — fixture applies master + sample by coordinates from the local
  repo: namespaces resolve, root values propagate, override fails (fail-fast, message asserted).
- No L1–L3 in this chunk (all layers proven in their chunks); no L3-style repo in this chunk.

## Acceptance criteria

- [ ] AC1 — `publishAllPublicationsToTestLocalRepository` publishes every artifact + plugin marker to the `-PtestRepoUrl` repo.
- [ ] AC2 — Fixture builds resolve master + sample by coordinates (no project dependencies).
- [ ] AC3 — Linked tree behaves: namespaces present, propagation works, immutable override fails.
- [ ] AC4 — `./gradlew check` stays green offline without running linking; `linkingTest` green after
      `testPublish` (documented CI order).

## Small steps (in order, each verifiable — one step, one commit after review)

- [x] **8.1 Publish wiring.** `maven-publish` on all modules (publications configured in-module) +
   conditional test repo from `-PtestRepoUrl`. Verify: repo contains every jar + marker.
2. **8.2 Linking module + tests.** `linking/` module, `linkingTest` suite proving AC2–AC3.
   Verify: `linkingTest` green (runs `testPublish` first).
3. **8.3 Docs.** README release-path paragraph + CHANGELOG entry describing exactly what the suite
   proves. Verify: prose matches tests.
4. **8.4 Full gate.** `./gradlew check --rerun-tasks` green *plus* `linkingTest` green in documented
   order; update `plans/README.md` (Chunk 8 → done). Verify: every suite executed.
5. **8.5 Final retrospective.** Record outcome across all chunks, amend `AGENTS.md` if needed.

## Verification

- `testPublish` output inspected (every jar + marker present).
- L4 proves coordinate-based consumption end to end.
- Reviewer approval of D1–D4 counts as verification for the linking shape.

## Review questions

1. Confirm D1–D4 as proposed (own module out of `check`, `maven-publish` to `build/local-repo`,
   all modules, separate task).
2. Confirm the release-path docs live in README (not a separate doc).
