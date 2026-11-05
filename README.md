# gradle-easy-plugin-2

Convention plugins for Gradle, built around one shared `easy` extension.

> **Status:** early planning — no published plugin yet. This README describes the *intended* consumer
> experience from `SPEC.md` so the API is designed consumer-first. It will grow with each implemented chunk.

## The idea

- One **core plugin** registers an `easy` extension container on **all** projects.
- **Contributor plugins** add sub-extensions under it, e.g. `easy.jvm`.
- One **master plugin** bundles core + a curated set of contributors. Put it on the classpath and the bundle
  is applied together — no per-contributor `apply` needed.
- **Third-party contributors** (yours or anyone's) apply alongside the master plugin and wire themselves into
  the same `easy` tree.
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
    id("com.example.easy.master") version "0.1.0"
    // optional third-party contributor alongside master:
    // id("com.example.easy.extra") version "0.1.0"
}

easy {
    // sub-extensions contributed by the bundled plugins, e.g.:
    // jvm { javaVersion.set(21) }
}

// child build.gradle.kts — inherits the root `easy` tree automatically;
// override or extend only what differs.
```

Build services (when available) are consumed through Gradle's shared-service mechanism; each service ships as
its own artifact so builds only depend on what they use. Details will be documented per contributor as they land.

## Requirements

- Gradle 8.x (exact minimum TBD in Chunk 1).
- Java 17+ to run the build (target toolchains per contributor, e.g. `easy.jvm`).

## Project conventions (for contributors to this repo)

See `AGENTS.md`. In short: plan → spec test → minimal implementation, `ProjectBuilder` unit tests by default,
`GradleRunner` only for higher-level interaction, Keep-a-Changelog + this README updated in the same change.

## Roadmap

Ordered build chunks — see `plans/00-high-level-plan.md` and the tracker in `plans/README.md`:

1. Build scaffolding + test infrastructure
2. Core `easy` extension plugin
3. Contributor plugin contract (`easy.jvm` as reference)
4. Root → child extension propagation (deep / shallow / immutable)
5. Master bundling plugin (classpath auto-apply)
6. Third-party contributor interop
7. Shared build-service APIs as separate artifacts
8. Published-artifact linking verification

## Changelog

See `CHANGELOG.md`.
