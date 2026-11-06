# gradle-easy-plugin-2

Convention plugins for Gradle, built around one shared `canon` extension.

> **Status:** early planning — no published plugin yet. This README describes the *intended* consumer
> experience from `SPEC.md` so the API is designed consumer-first. It will grow with each implemented chunk.

## The idea

- One **core plugin** registers a `canon` extension container on **all** projects.
- **Contributor plugins** add sub-extensions under it, e.g. `canon.versioning`.
- One **master plugin** (`com.mreil.canon`) bundles core + the curated contributors (currently just
  `canon.versioning`). Apply the single id and the bundle is applied together — no per-contributor
  `apply` needed. Bundle and contributors version in lockstep.
- **Third-party contributors** apply their own plugin id next to the master bundle and wire into the
  same `canon` tree. See `CONTRIBUTING.md` to write one.
- Values set on the **root** project flow into **children** once the root is evaluated: `Property`
  values are inherited as conventions, values marked `IMMUTABLE` additionally fail the build if a
  child overrides them. See `CONTRIBUTING.md` for the exact semantics.

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

## Requirements

- Gradle 8.x (exact minimum TBD in Chunk 1).
- Java 17+ to run the build (target toolchains per contributor).

## Roadmap

Ordered build chunks — see `plans/00-high-level-plan.md` and the tracker in `plans/README.md` for
status. Everything so far is verified against a local test repo, never a live one.

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
