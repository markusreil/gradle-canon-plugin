# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html) once published.

## [Unreleased]

### Added

- `com.mreil.canon.codemeta` contributor: `codemeta.json` file model (`canon-codemeta-api` with
  the full CodeMeta term set), `initCodemeta` task (creates the default file, then fails asking
  the user to fill it in), shared `CodemetaService`; holders stay configuration-only.

## [0.0.1] - 2026-11-06

### Added

- `SPEC.md` as product source of truth (core `easy` extension, contributors, master plugin, root→child
  propagation, shared build services, test strategy).
- `AGENTS.md` working agreements (plan-first, spec-tests-first, small chunks, Farley four-layer acceptance
  model, `ProjectBuilder`-first testing, docs-as-done).
- `README.md` consumer-facing vision of the plugin (target DSL, master/contributor model, roadmap).
- `plans/` tracker and high-level plan (`00-high-level-plan.md`) ordering the build into 8 chunks.
- Core plugin `com.mreil.canon.core`: `canon` extension container registered on all projects.
- Reference contributor `com.mreil.canon.versioning`: `canon.versioning` sub-extension managing versioning
  schemes (default `semver`, version parsed from the root project's version via semver4j); establishes the
  contributor contract (require `canon`, fail fast otherwise, idempotent re-apply, unique names).
- Root → child propagation: core copies `canon` sub-extension values root → children (`Property` values
  inherited as conventions, explicit `@Propagate(SHALLOW | IMMUTABLE | DEEP)` semantics in the new
  `com.mreil.canon:canon-api` module, immutable drift check at the end of configuration). Reference
  `scheme` is now a `Property<String>` marked `IMMUTABLE`.
- `com.mreil.canon` master bundle: applying the single id wires core plus the curated contributors
  (currently `canon.versioning`); versions move in lockstep.
- Third-party interop proven by the `extra` sample: foreign holders propagate, missing `canon`
  fails fast, clashes fail fast; sample stays api-only and never joins the bundle.
- Shared build services: `com.mreil.canon:canon-versioning-api` ships `SemverService` (parse +
  snapshot/release, version as a service parameter); `com.mreil.canon.publish` consumes it without
  depending on versioning. Started, not finished.
- Linking verification: `maven-publish` on every module, test-publish to a CLI-given local repo,
  `linkingTest` consumes master + third-party by coordinates (namespaces, propagation, override
  failure all proven on published artifacts).
