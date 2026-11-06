# 07 — Shared build-service APIs as separate artifacts

Status: **approved** — D1–D3 settled. Work proceeds in step order.

## Goal

Prove cross-contributor communication through a shared Gradle build service: the new `publish`
contributor consumes a semver-parsing service owned by `versioning` without depending on the whole
versioning contributor. The service API ships in its own build artifact so builds depend only on what
they use. The `publish` plugin itself gets started, not finished — just enough to prove the public
API (tiered routing and the rest stay backlog).

## Open (parked, not decided)

- Consumer access pattern: fail fast if the service is missing vs lazy `Provider<Service>`?
  Split service into interface + impl? Service creation / consumption helper as a fixed pattern in
  `core-api`? All parked — needs a bigger sample size to prove. Do not decide yet.

## Non-goals (explicitly deferred)

- Real convention logic inside contributors.
- Version alignment between service artifacts and contributors.
- Publishing (Chunk 8 owns the release path).

## Carried in (settled inputs, not open questions)

Chunks 3–6 settled the contributor contract, propagation timing, the bundle shape, and third-party
interop. The service work builds on those contracts unchanged. Anything outside them fails fast
rather than growing plan scope (AGENTS.md rule 9). DEEP merge stays deferred.

## Design (pick: `publish` consumes versioning's semver service)

- New module `plugins/canon-versioning-api` (group `com.mreil.canon`): the versioning public API
  owned versioning-side. It lifts the dependency-light `parseVersion` companion function untouched and
  exposes it as a shared build service (parsed version plus snapshot/release distinction — exactly what
  tiered artifact routing needs first).
- New module `plugins/canon-publish` (id `com.mreil.canon.publish`): the `publish` contributor, started
  not finished — registers a `publish` holder under `canon` and consumes the semver service to decide
  routing. Full maven-publish wiring, POM coordinates, and tiered routing stay backlog.
- Contributor B (`publish`) depends only on the service module, never on versioning. Registration via
  Gradle's shared-service mechanism; lifecycle and isolation rules proven by tests, not just asserted.

## Decisions proposed (confirm or overrule in review)

| # | Decision | Proposal | If you prefer otherwise |
|---|----------|----------|-------------------------|
| D1 | Service pick | `publish` (needs semver most, high on the backlog); `parseVersion` lifts untouched into the service. Settled | Fix the pick in this plan now |
| D2 | Artifact shape | One new module for the service API + impl, owned versioning-side. Settled | Service inside an existing module |
| D3 | Consumer coupling | B depends on the service module only, verified by a dependency assertion like AC4's. Settled | Looser coupling, documented |

## Acceptance-test layering (uses the Chunk 1 model, no changes)

- **L1 (bulk):** `ProjectBuilder` — B consumes A's service in a unit test build without A's
  contributor on the classpath; lifecycle and isolation covered at unit level.
- **L2 (one test):** `GradleRunner` — test build wires both contributors; the service result is
  observable in build output.
- No L3/L4 in this chunk.

## Acceptance criteria

- [ ] AC1 — Contributor B consumes contributor A's shared service without depending on A (L1 direct +
      L2 by ids).
- [ ] AC2 — Service registration, lifecycle, and isolation rules proven by tests.
- [ ] AC3 — Service ships in its own artifact; no contributor gains a whole-contributor dependency.
- [ ] AC4 — `./gradlew check` green (all suites, all modules); no new warnings; config-cache reuse on
      the second run.

## Small steps (in order, each verifiable — one step, one commit after review)

- [x] **7.0 Plugin backlog (maintainer).** Backlog written in `00-high-level-plan.md`; pick recorded: `publish` (needs semver most). Done.
- [x] **7.1 Service shape (review).**: module layout, service
   API, registration and isolation rules, which tests prove what. No code. Verify: review approves.
- [x] **7.2 Service module.** New module with the service API + implementation. Verify: compiles.
- [x] **7.3 L1 proof.** Unit tests for AC1–AC3. Verify: green.
- [x] **7.4 L2 proof.** `GradleRunner` test proving the wired experience. Verify: green.
- [x] **7.5 Docs.** README service section + CHANGELOG entry describing exactly what the tests prove.
   Verify: prose matches tests.
- [x] **7.6 Full gate.** `./gradlew check --rerun-tasks` green; update `plans/README.md` (Chunk 7 →
   done). Verify: every suite executed.
- [x] **7.7 Chunk 7 retrospective (required before Chunk 8).** Outcome: one AGENTS.md amendment (one
  public API per plugin: `canon-<name>-api`). Lessons banked: read the version lazily (apply-time reads
  see the pre-script value — service params take a provider, validation moves to `afterEvaluate`, which
  is acceptable for up-to-date properties while live collections / `on*Added` cover almost everything
  else); every
  new module with a Gradle-touching main must join the L2 fixture classpaths by hand (TestKit has no
  transitivity — versioning-api jar added to versioning/master/sample/publish fixtures).

## Verification

- `./gradlew check` exit 0 with every suite in every module executing (AC4/7.6).
- L1 proves decoupled consumption at unit level; L2 proves the wired experience on a real build.
- Reviewer approval of D1–D3 counts as verification for the service shape.

## Review questions

1. Confirm D1–D3 as proposed (backlog-driven pick with `parseVersion` fallback, separate module, decoupled consumer).
2. Confirm 7.0/7.1 as two review gates before any service code.
