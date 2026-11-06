# Development

How to work on this repo. The process rules live in `AGENTS.md`; this file is the practical summary.

## Workflow

Plan before code (`plans/NN-<slug>.md` + tracker in `plans/README.md`), spec test first, smallest
correct change. Production and test sources are Kotlin; versions come from the `libs` catalog; no
top-level `test*` configurations (dependencies live in the owning `JvmTestSuite`).

## Test layers

- Unit tests (`ProjectBuilder`) carry the bulk — new behavior lands with a failing test first.
- `GradleRunner` functional tests cover higher-level interaction only; fixtures are explicit strings
  written at test runtime.
- `linkingTest` consumes published artifacts by coordinates and stays out of `check`.

## Verification

- `./gradlew check` — diagnostics, all unit + functional suites, coverage, Spotless, detekt.
  Green and offline-friendly by design.
- `./gradlew :linking:linkingTest -PtestRepoUrl=<dir>` — publishes all jars + plugin markers to that
  local Maven repo and consumes master + third-party contributor by coordinates from it. The release
  path, minus the upload.
- KDoc is enforced on public API (`UndocumentedPublic*` in `config/detekt.yml`); test classes carry
  a one-line "what this proves" doc. KDoc precedes annotations.

## Docs are part of done

- `README.md` — consumer view only.
- `CONTRIBUTING.md` — contributor-plugin contract.
- `CHANGELOG.md` — every user-visible change under `Unreleased`.
- `plans/` — chunk plans + tracker, updated when a chunk starts and lands.
