# 06 — Third-party contributor interop

Status: **approved** — D1–D4 settled as proposed. The sample stays a fixture module and is never
added to master's dependencies. Work proceeds in step order.

## Goal

Prove a non-bundled contributor applies alongside master and wires into the same `canon` tree:
its namespace appears, its values propagate, and a name clash with a bundled contributor fails with
a clear error. The sample lives in-repo but depends only on `canon-api` (plus Gradle API) — the
same classpath a real third party sees.

## Non-goals (explicitly deferred)

- Publishing the sample anywhere; it is a test fixture with its own module, never a shipped artifact
  story (Chunk 8 owns published-artifact verification).
- Version alignment between third parties and the bundle.
- New propagation semantics (DEEP still deferred).

## Carried in (settled inputs, not open questions)

Chunks 3–5 settled the contributor contract, propagation timing, and the bundle shape. The sample
follows the contract exactly (require `canon`, fail fast otherwise, idempotent re-apply, unique
names, concrete holder, Gradle property types, no `Project` constructor `val`). Anything outside
those contracts fails fast rather than growing plan scope (AGENTS.md rule 9).

## Design

New module `plugins/canon-thirdparty-sample` (group `com.mreil.canon`, id
`com.example.canon.extra` — deliberately outside `com.mreil.canon.*` plugin-id space to prove ids
are just coordinates): `ExtraExtension` holder with one `Property<String>` (`mode`, convention
`"auto"`, explicit `@Propagate(SHALLOW)` to exercise the non-immutable path through a foreign
holder) registered as `extra` under `canon` on all projects.

Notes on the shape:

- The sample's `implementation` dependencies are `canon-api` only — no core, no versioning, no master.
  Its test suites may depend on siblings for fixtures (per AGENTS.md); its `main` must not.
- Ordering: the sample registers holders when applied, like every contributor; propagation runs at
  root `afterEvaluate`, so both apply orders (master-first, sample-first) wire the same tree. L2
  covers master-first; L1 covers both orders.
- Collision: the sample's `register` repeats the contributor contract's `findByName` pre-check. A
  second sample (or the sample applied under a taken name) asserting the clash proves the message, not
  Gradle's generic duplicate-extension error. The colliding-names test uses a same-name twin holder in
  the test source set — no second module needed.
- "Late registration" means applied later in the same script (after master), not after evaluation:
  everything applied during configuration registers before the `afterEvaluate` copy pass. Post-
  evaluation registration cannot happen in a normal build and gets no machinery.

## Decisions proposed (confirm or overrule in review)

| # | Decision | Proposal | If you prefer otherwise |
|---|----------|----------|-------------------------|
| D1 | Sample home | New module `plugins/canon-thirdparty-sample`, api-only `main` deps, id `com.example.canon.extra`. Approved | Fixture-only (test-sourceset holder, no module) |
| D2 | Sample state | One `Property<String>` (`mode`, convention `"auto"`), explicit `@Propagate(SHALLOW)`. Approved | Immutable or inferred semantics |
| D3 | Ordering coverage | L2 master-first; L1 master-first plus sample-without-canon fails fast (a contributor requires `canon` — sample-first is a contract error, not an order). Approved | L2 both orders too |
| D4 | Collision proof | Same-name twin holder in the sample's test sourceset asserting the clash message. Approved | Second sample module |

## Acceptance-test layering (uses the Chunk 1 model, no changes)

- **L1 (bulk):** `ProjectBuilder` — master + sample on root (+child): `canon.extra` present on both,
  root `mode` propagates to child; sample-without-`canon` fails fast; same-name twin fails with the clash
  message; sample `main` has no core/master dependency (asserted via `runtimeClasspath`).
- **L2 (one test):** `GradleRunner` — test build applies master id + sample id, root sets `mode`,
  child guards the inherited value, `help` green. Reuses the combined TestKit classpath pattern
  (own metadata + core/versioning/api/sample jars).
- No L3/L4 in this chunk.

## Acceptance criteria

- [ ] AC1 — Master + sample coexist: `canon.versioning` and `canon.extra` resolve on root and child
      (L1 direct + L2 by id).
- [ ] AC2 — Foreign-holder values propagate: root-set `mode` is visible in children (L1 direct + L2
      guard).
- [ ] AC3 — Name clash fails fast with the contract message (L1, same-name twin).
- [ ] AC4 — Sample ships api-only: no core/versioning/master classes on its main compile classpath.
- [ ] AC5 — `./gradlew check` green (all suites, all modules); no new warnings; config-cache reuse on
      the second run.

## Small steps (in order, each verifiable — one step, one commit after review)

- [x] **6.1 Sample module.** New module + `ExtraPlugin`/`ExtraExtension` per D1–D2; api-only `main` deps.
   Verify: compiles (AC4 by construction, asserted in 6.2).
- [x] **6.2 L1 proof.** `ProjectBuilder` matrix for AC1–AC4 (presence, propagation, both orders, clash,
   api-only assertion). Verify: `:plugins:canon-thirdparty-sample:test` green.
3. **6.3 L2 proof.** One `GradleRunner` test (master + sample ids, inherit-guard green). Verify:
   `:plugins:canon-thirdparty-sample:functionalTest` green.
4. **6.4 Docs.** README third-party paragraph rewritten to implemented reality + CHANGELOG entry.
   Verify: prose describes exactly what the tests prove.
5. **6.5 Full gate.** `./gradlew check --rerun-tasks` green (all suites, all modules); update
   `plans/README.md` (Chunk 6 → done). Verify: every suite executed.
6. **6.6 Chunk 6 retrospective (required before Chunk 7).** Record outcome, amend `AGENTS.md` if needed.

## Verification

- `./gradlew check` exit 0 with every suite in every module executing (AC5/6.5).
- L1 proves coexistence, propagation, ordering, clash, and api-only shape at unit level; L2 proves
  the two-id author experience on a real build.
- Reviewer approval of D1–D4 counts as verification for the interop shape.

## Review questions

1. Confirm D1–D4 as proposed (sample module with foreign id, SHALLOW `mode`, ordering split, twin-holder
   clash proof).
2. Confirm the sample stays a fixture module and never a publishing story in this chunk.
