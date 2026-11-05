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
  same `canon` tree — no core classes needed, only `canon-api`. The reference is the `extra` sample:
  its `mode` propagates root → children, both apply orders work, and name clashes fail fast with a
  contract message.
- Values set on the **root** project flow into **children** once the root is evaluated: `Property`
  values are inherited as conventions (an absent root value leaves children untouched), values marked
  `IMMUTABLE` additionally fail the build if a child overrides them. Collection properties
  (`ListProperty`, …) are reserved for deep merge (not implemented yet); anything else only
  propagates with an explicit `@Propagate` mark.
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
   or the master bundle — failing fast with a clear message otherwise. Ordering is the author's job:
   no deferred (`withPlugin`) registration, so a missing core fails loudly instead of silently doing
   nothing. Non-bundled plugins must be applied after master (or core). Never apply core yourself; you need no core classes on your classpath (Gradle API only).
3. Register your holder via `canon.extensions.create("<name>", <Type>::class.java)` on every project.
4. Names are unique per `canon` tree: if the name is taken by a different implementation, fail fast; if it is
   your own holder from a re-apply, keep the existing instance.
5. Re-applying your plugin is harmless.

### Holder and property shape

- Holders are concrete `open` classes, never `abstract`: the field-targeted `@Propagate` annotation and
  the reflection copy pass both need real backing fields.
- Take `@Inject constructor(objects: ObjectFactory)` and create every public property explicitly, e.g.
  `objects.property(String::class.java).convention("semver")`. Public state uses Gradle lazy property
  types (`Property`, `ListProperty`, …) — anything else needs a good reason.
- Rule of thumb: the holder configures the plugin; internal state stays out of it. Most contributors
  carry an `enabled` switch (`Property<Boolean>`, convention `true`) that gates their wiring.
- Never take `Project` as a constructor `val`; inject `ObjectFactory` and let the plugin wire the project
  (e.g. an `internal lateinit var` assigned at registration).

### Propagation

Values flow root → children automatically; you only declare semantics:

- Mark propagatable state with `@Propagate(...)` (`SHALLOW`, `IMMUTABLE`; `DEEP` is reserved and
  currently rejected with a clear error). Without the annotation, `Property` fields default to `SHALLOW`
  and collection properties to `DEEP`; anything else fails fast — annotate it explicitly.
- Only public state propagates: fields with a public accessor (Kotlin-internal and private state stays
  local). Static and synthetic fields are ignored.
- `IMMUTABLE` copies like `SHALLOW`, then fails the build at the end of configuration if a child changed
  the value, naming the project, the property, and the root-owned value.

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
