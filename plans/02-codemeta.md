# 02 — codemeta plugin (file model, init task, public service)

Status: **approved** — D1–D5 settled. Work proceeds in step order.

## Goal

A `codemeta` contributor that owns the repo's `codemeta.json`: a file model parsed with
kotlinx.serialization, a task that creates the initial file when absent, and a public build
service exposing the info. This unblocks publish (POM fields Gradle doesn't model, which jReleaser
also requires).

## Non-goals (explicitly deferred)

- Real POM/jReleaser wiring (chunks 3–5 consume this).
- Validating the file beyond parsing (fail on unreadable/invalid JSON only).
- Any propagated state — codemeta is repo-root data, not per-project configuration.

## Carried in (settled inputs, not open questions)

Contributor contract, holder shape (`enabled` switch, Gradle property types, no `Project` val),
fail-fast ordering, one public API per plugin (`canon-<name>-api`), service patterns stay open
(plan 07 carried decisions). Anything outside those fails fast (AGENTS.md rule 9).

## Design

- New modules `plugins/canon-codemeta` (id `com.mreil.canon.codemeta`) + `plugins/canon-codemeta-api`
  (model + service). Main deps are api-only (`canon-api`, kotlinx.serialization-json); no core.
- `codemeta.json` lives at the root project dir. The plugin registers a `codemeta` holder (with the
  usual `enabled` switch) on all projects like every contributor, but reads the single root file —
  no propagation needed since there is nothing per-project to copy.
- `CodemetaService` (shared build service, registered by the plugin): parses the file once, exposes
  the model; unreadable or invalid JSON fails the build naming the file. Created on demand — no
  eager I/O at apply time.
- `initCodemeta` task (exact name open to review): writes a minimal valid `codemeta.json` when none
  exists; refuses to overwrite an existing file (fail fast, no silent merge).
- kotlinx.serialization, not Jackson — smaller, Kotlin-native, explicit schema via `@Serializable`
  model covering the fields POM/jReleaser need (name, description, authors, licenses, repo URLs,
  version stays Gradle-owned).

## Decisions proposed (confirm or overrule in review)

| # | Decision | Proposal | If you prefer otherwise |
|---|----------|----------|-------------------------|
| D1 | Module shape | Plugin + `-api` modules per the one-API rule; service interface in api, impl alongside. Settled | Single module |
| D2 | Serialization | kotlinx.serialization-json (explicit catalog entry). Settled | Jackson |
| D3 | Init task | `initCodemeta`: runs first; creates the stub when absent, then fails the build telling the user to fill it in. Settled | Overwrite/merge, different name |
| D4 | Scope | Holder on all projects (contract uniformity), single root file, no propagation. Settled | Root-only registration |
| D5 | Service surface | Parsed model access + `exists()`; nothing more until publish needs it. Settled | Richer API now |

## Acceptance-test layering (uses the Chunk 1 model, no changes)

- **L1 (bulk):** `ProjectBuilder` — plugin applies (holder present, `enabled` default true);
  service parses a fixture file (model fields visible); garbage JSON fails with the file named;
  init logic: absent file gets a stub, present file is left alone.
- **L2 (two tests):** `GradleRunner` — (a) `initCodemeta` creates the stub then fails the build
  telling the user to fill it in; a filled-in file configures green; (b) the service resolves a
  fixture `codemeta.json` on a real build.
- No L3/L4 in this chunk.

## Acceptance criteria

- [ ] AC1 — `codemeta.json` model parses via the service; garbage fails naming the file (L1 + L2).
- [ ] AC2 — `initCodemeta` creates the stub when absent then fails notifying the user; a present
  file is left alone (L1 + L2).
- [ ] AC3 — Service ships in its own artifact; codemeta main depends only on api + serialization.
- [ ] AC4 — `./gradlew check` green (all suites, all modules); no new warnings; config-cache reuse on
      the second run.

## Small steps (in order, each verifiable — one step, one commit after review)

- [x] **2.1 API module.** Model (`@Serializable`), service interface + impl, parse + `exists`.
   Verify: compiles.
- [x] **2.2 Plugin + init task.** Holder, registration, `initCodemeta`, service wiring. Verify: compiles.
- [x] **2.3 L1 proof.** Unit matrix for AC1–AC3. Verify: `:plugins:canon-codemeta:test` green.
- [x] **2.4 L2 proof.** Two `GradleRunner` tests (init create/refuse + service on a real build).
   Verify: `:plugins:canon-codemeta:functionalTest` green.
5. **2.5 Docs.** README/CONTRIBUTING/CHANGELOG touch-ups describing exactly what the tests prove.
   Verify: prose matches tests.
6. **2.6 Full gate.** `./gradlew check --rerun-tasks` green; update high-level plan (Chunk 2 → done).
   Verify: every suite executed.
7. **2.7 Chunk 2 retrospective.** Record outcome, amend `AGENTS.md` if needed.

## Verification

- `./gradlew check` exit 0 with every suite in every module executing (AC4/2.6).
- L1 proves parsing, fail-fast, and api-only shape; L2 proves the task and the wired service.
- Reviewer approval of D1–D5 counts as verification for the codemeta shape.

## Review questions

1. Confirm D1–D5 as proposed (api split, kotlinx, create-if-absent, all-projects holder, minimal service).
2. Confirm `initCodemeta` as the task name.
