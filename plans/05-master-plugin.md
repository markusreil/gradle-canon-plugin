# 05 — Master bundling plugin (classpath auto-apply)

Status: **approved** — D1–D4 settled as proposed. Work proceeds in step order.

## Goal

One plugin id for build authors: applying the master bundle puts core plus the curated contributor set
on the project with no per-contributor `apply` statements. Contributors keep their standalone ids and
contract; "bundled" stays release coordination, not a second contract (Chunk 3 D3). Applying master to
the root wires the whole `canon` tree exactly as if each plugin had been applied by hand.

## Non-goals (explicitly deferred)

- Third-party contributors alongside master (Chunk 6) — master must not special-case out-of-tree ids.
- Optional/selectable contributors in the bundle; the bundle is fixed (core + versioning).
- Version ranges or alignment checks between bundle and contributors.
- DEEP merge (still deferred to its chunk; unaffected by bundling).

## Carried in (settled inputs, not open questions)

Chunk 4 closed all its decisions (D1–D7); nothing open carries into this chunk. Master builds on two
settled contracts: the contributor contract (require `canon`, fail fast, idempotent re-apply, unique
names) and propagation timing (copy at root `afterEvaluate`, drift check at `projectsEvaluated`).
Anything outside those contracts should fail fast in code rather than grow a new rule here.

## Design

New module `plugins/canon-master` (group `com.mreil.canon`, id `com.mreil.canon`):

```kotlin
class CanonMasterPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(CanonPlugin::class.java)
        project.pluginManager.apply(VersioningPlugin::class.java)
    }
}
```

Notes on the shape:

- Master applies by class, not by id: no plugin-id resolution is needed, unit tests stay dependency-light,
  and the ids remain purely a build-author concern (resolved via the published markers / TestKit
  classpath as in 4.5).
- Master depends on core + versioning as shipped `implementation` dependencies — the bundle really ships
  its contributors, unlike test fixtures which stay in `testing { suites { … } }`.
- Ordering is safe by construction: everything master applies runs during root evaluation, so holders
  exist before core's root-`afterEvaluate` copy pass. Contributors applied later in the same script (the
  Chunk 6 shape) register before `afterEvaluate` too — no special casing.
- Master applies to the **root** project (like every contributor); each bundled plugin's own
  `allprojects` wiring reaches the children. Applying master to a non-root project is out of contract —
  core's `else` branch registers locally, contributors follow their own rules.

## Decisions proposed (confirm or overrule in review)

| # | Decision | Proposal | If you prefer otherwise |
|---|----------|----------|-------------------------|
| D1 | Apply mechanism | `pluginManager.apply(XxxPlugin::class.java)` per bundled plugin, in dependency order (core first). Approved | Apply by id (needs id resolution in every test layer) |
| D2 | Bundle contents | Fixed: core + versioning, nothing else. A new contributor joins the bundle in its own chunk's step. Approved | Selectable/optional contributors |
| D3 | Versioning | Lockstep: one repo version for bundle and contributors, no alignment checks. Approved | Independent versions + range checks |
| D4 | Master id | `com.mreil.canon`. Approved | Another id |

## Acceptance-test layering (uses the Chunk 1 model, no changes)

- **L1 (bulk):** `ProjectBuilder` — apply master to root (+child): `canon` + `canon.versioning` present on
  both; propagation still flows through the bundle (root `scheme = "custom"` + `copyTree` → child reads
  `"custom"`, drift recorded).
- **L2 (one test):** `GradleRunner` — test build applies only the master id: `help` green and a child
  guard reads the inherited `scheme`. Reuses the combined TestKit classpath pattern from 4.5
  (own metadata + core/versioning/api jars).
- No L3/L4 in this chunk.

## Acceptance criteria

- [ ] AC1 — Applying only `com.mreil.canon` to the root yields `canon` + `canon.versioning` on root and
      all children (L1 direct + L2 by id).
- [ ] AC2 — Propagation is unaffected by bundling: root-set `scheme` is visible in children with an
      immutability record (L1 direct).
- [ ] AC3 — Master ships its contributors: master module has shipped (non-test) dependencies on core +
      versioning; no contributor depends on master.
- [ ] AC4 — `./gradlew check` green (all suites, all modules); no new warnings; config-cache reuse on the
      second run.

## Small steps (in order, each verifiable — one step, one commit after review)

- [x] **5.1 Master module + plugin.** New module `plugins/canon-master` (group `com.mreil.canon`, id
   `com.mreil.canon`): `CanonMasterPlugin` applying core then versioning; shipped `implementation`
   dependencies on both. Verify: compiles.
- [x] **5.2 L1 proof.** `ProjectBuilder` matrix for AC1–AC2 (presence on root/child, propagation through the
   bundle). Verify: `:plugins:canon-master:test` green (in-suite dep on core + versioning for fixtures).
- [x] **5.3 L2 proof.** One `GradleRunner` test applying only the master id (inherit-guard green). Verify:
   `:plugins:canon-master:functionalTest` green.
- [x] **5.4 Docs.** README master section rewritten to implemented reality (id, fixed bundle, lockstep) +
   CHANGELOG entry. Verify: prose describes exactly what the tests prove.
- [x] **5.5 Full gate.** `./gradlew check --rerun-tasks` green (all suites, all modules); update
   `plans/README.md` (Chunk 5 → done). Verify: every suite executed.
- [x] **5.6 Chunk 5 retrospective (required before Chunk 6).** Outcome: nothing came up — keep everything as is.

## Verification

- `./gradlew check` exit 0 with every suite in every module executing (AC4/5.5).
- L1 proves bundle presence + propagation-through-bundle at unit level; L2 proves the single-id
  author experience on a real build.
- Reviewer approval of D1–D4 counts as verification for the bundle shape.

## Review questions

1. Confirm D1–D4 as proposed (apply-by-class, fixed bundle, lockstep versions, `com.mreil.canon` id).
2. Confirm the "Carried in" stance: no open decisions enter this chunk; anything outside the settled
   contracts fails fast rather than growing plan scope.
