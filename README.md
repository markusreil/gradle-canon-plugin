# gradle-easy-plugin-2

Convention plugins for Gradle, built around one shared `canon` extension.

> **Status:** early planning — no published plugin yet. This README describes the *intended* consumer
> experience from `SPEC.md` so the API is designed consumer-first. It will grow with each implemented chunk.

## The idea

- One **core plugin** registers a `canon` extension container on **all** projects.
- **Contributor plugins** add sub-extensions under it, e.g. `canon.versioning`.
- One **master plugin** bundles core + a curated set of contributors. Put it on the classpath and the bundle
  is applied together — no per-contributor `apply` needed.
- **Third-party contributors** (yours or anyone's) apply alongside the master plugin and wire themselves into
  the same `canon` tree.
- Configuration written on the **root** project flows into **children**, with per-value semantics: some values
  are extended (deep copy), some overwritten (shallow copy), some locked (immutable).
- Contributors can expose **shared build services** (separate artifacts) that other contributors consume.

## Intended usage (target API — not yet implemented)

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories { gradlePluginPortal(); mavenCentral() }
}

// root build.gradle.kts
plugins {
    id("com.mreil.canon") version "0.1.0"
    // optional third-party contributor alongside master:
    // id("com.example.canon.extra") version "0.1.0"
}

canon {
    // sub-extensions contributed by the bundled plugins, e.g.:
    // versioning { ... }
}

// child build.gradle.kts — inherits the root `canon` tree automatically;
// override or extend only what differs.
```

Build services (when available) are consumed through Gradle's shared-service mechanism; each service ships as
its own artifact so builds only depend on what they use. Details will be documented per contributor as they land.

## Writing a contributor plugin

The contract, demonstrated by the reference contributor `com.mreil.canon.versioning`:

1. Coordinates live under `com.mreil.canon.*`; the sub-extension name matches the id suffix (`versioning`).
2. Apply your plugin to the **root** project and require `canon` to be present — put there by the build author
   or the master bundle — failing fast with a clear message otherwise. Never apply core yourself; you need no
   core classes on your classpath (Gradle API only).
3. Register your holder via `canon.extensions.create("<name>", <Type>::class.java)` on every project.
4. Names are unique per `canon` tree: if the name is taken by a different implementation, fail fast; if it is
   your own holder from a re-apply, keep the existing instance.
5. Re-applying your plugin is harmless.

`canon.versioning` manages versioning schemes (default `semver`, parsed from the root project's version via
semver4j) and doubles as the candidate for testing the external build-service API later.

## Requirements

- Gradle 8.x (exact minimum TBD in Chunk 1).
- Java 17+ to run the build (target toolchains per contributor).

## Project conventions (for contributors to this repo)

See `AGENTS.md`. In short: plan → spec test → minimal implementation, `ProjectBuilder` unit tests by default,
`GradleRunner` only for higher-level interaction, Keep-a-Changelog + this README updated in the same change.

## Roadmap

Ordered build chunks — see `plans/00-high-level-plan.md` and the tracker in `plans/README.md`:

1. Build scaffolding + test infrastructure
2. Core `canon` extension plugin
3. Contributor plugin contract (`canon.versioning` as reference)
4. Root → child extension propagation (deep / shallow / immutable)
5. Master bundling plugin (classpath auto-apply)
6. Third-party contributor interop
7. Shared build-service APIs as separate artifacts
8. Published-artifact linking verification

## Changelog

See `CHANGELOG.md`.
