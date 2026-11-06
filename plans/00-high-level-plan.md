# High-level plan

Status: **in review** — goal filled in by maintainer; chunking below is a proposal.

## Goal

<!-- What are we building, and why? One paragraph. -->
High-level goal is to get this to a dogfood-ready stage. This means:
* implement publish plugin
  * create mavenJava publication automatically (careful: plugin projects create it, regular java-* projects don't)
  * auto-set correct POM coordinates
  * define set of repos on root, applied for every applicable child (mostly: is this a java project). question: apply maven-publish automatically or opt-in?
  * jreleaser publishing to maven central
  * tiered repo design (snapshot, release, promotion (need guard and ability to retroactively publish because of tight central limits))
  * testRepo (our linking publish) becomes a feature.
* prerequisite: codemeta plugin (do before publish)
  * maven POM supports fields (and jreleaser requires them) that are not part of the gradle project model. codemeta is a good
    way to support that.
  * kotlinx serialization over jackson
  * task that creates initial file if not present.
  * public service that gives access to the codemeta info
* github actions configuration (do before everything else?)
  * build main on every change
  * build on branches for each PR
* not in scope: release, can be done manually

## Context

v0.0.1 tagged: core + versioning + master bundle + third-party sample + semver service + publish
starter + linking verification, all green. Test-publish today is throwaway infra (`-PtestRepoUrl` to a
scratch dir); this arc turns the publish path into the real thing. Codemeta does not exist yet and
blocks POM coordinates, so it comes before publish. CI does not exist yet; it goes first so every
later chunk is verified per change.

## Chunks (fill in as they emerge)

| #  | Chunk | Plan file | Status | Notes |
|----|-------|-----------|--------|-------|
| 1  | CI: build main on every change, branches on every PR | `01-ci.md` | done | Green on first runs; retro clean |
| 2  | codemeta plugin: file model, init task, public service | `02-codemeta.md` | done | Model + service + init task green; full gate green |
| 3  | publish plugin core: mavenJava auto, POM coords from codemeta, repo set | `03-publish-core.md` | todo | Includes auto vs opt-in decision |
| 4  | Tiered routing: snapshot/release/promotion guard + retroactive publish | `04-tiered-routing.md` | todo | Central limits shape the guard |
| 5  | jReleaser to Maven Central | `05-jreleaser.md` | todo | |
| 6  | testRepo as a feature | `06-test-repo.md` | todo | Today's throwaway infra, productized |

Statuses: `todo` → `in progress` → `done`.

## Non-goals (explicitly deferred)

- Release automation (manual release stays; see TODO backlog `release` item).
- Everything in `TODO.md` not named above.

## Verification

- `./gradlew check` green on main and on every PR (chunk 1).
- First done checkpoint after chunk 3: this repo self-publishes through its own publish plugin
  to a private Maven staging repo (coordinates + credentials provided by maintainer at that point).
  Then reassess remaining work.
