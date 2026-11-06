# Contributing a contributor plugin

How to write a bundled or third-party contributor for the `canon` tree. The contract is demonstrated
by the reference contributor `com.mreil.canon.versioning` and the `extra` sample.

## The contract

1. Coordinates live under `com.mreil.canon.*`; the sub-extension name matches the id suffix (`versioning`).
   Third-party ids live outside that space (e.g. `com.example.canon.extra`) so test publishes can never
   collide with a live artifact.
2. Apply your plugin to the **root** project and require `canon` to be present — put there by the build author
   or the master bundle — failing fast with a clear message otherwise. Ordering is the author's job:
   no deferred (`withPlugin`) registration, so a missing core fails loudly instead of silently doing
   nothing. Non-bundled plugins must be applied after master (or core). Never apply core yourself; you need no core classes on your classpath (Gradle API only).
3. Register your holder via `canon.extensions.create("<name>", <Type>::class.java)` on every project.
4. Names are unique per `canon` tree: if the name is taken by a different implementation, fail fast; if it is
   your own holder from a re-apply, keep the existing instance.
5. Re-applying your plugin is harmless.

## Holder and property shape

- Holders are concrete `open` classes, never `abstract`: the field-targeted `@Propagate` annotation and
  the reflection copy pass both need real backing fields.
- Take `@Inject constructor(objects: ObjectFactory)` and create every public property explicitly, e.g.
  `objects.property(String::class.java).convention("semver")`. Public state uses Gradle lazy property
  types (`Property`, `ListProperty`, …) — anything else needs a good reason.
- Rule of thumb: the holder configures the plugin; internal state stays out of it. Most contributors
  carry an `enabled` switch (`Property<Boolean>`, convention `true`) that gates their wiring.
- Never take `Project` as a constructor `val`; inject `ObjectFactory` and let the plugin wire the project
  (e.g. an `internal lateinit var` assigned at registration).

## Propagation

Values flow root → children automatically; you only declare semantics:

- Mark propagatable state with `@Propagate(...)` (`SHALLOW`, `IMMUTABLE`; `DEEP` is reserved and
  currently rejected with a clear error). Without the annotation, `Property` fields default to `SHALLOW`
  and collection properties to `DEEP`; anything else fails fast — annotate it explicitly.
- Only public state propagates: fields with a public accessor (Kotlin-internal and private state stays
  local). Static and synthetic fields are ignored.
- `IMMUTABLE` copies like `SHALLOW`, then fails the build at the end of configuration if a child changed
  the value, naming the project, the property, and the root-owned value.

## Shared build services

Cross-cutting logic ships as a build service in its own artifact, owned by one contributor and
consumed by others without a contributor-to-contributor dependency. Reference: `canon-versioning-api`
(`SemverService` — parse plus snapshot/release distinction, version supplied as a service parameter by
the versioning plugin, fail-fast on garbage). Each plugin keeps one public API; service consumer
patterns (fail-fast vs lazy, interface split) stay open until more consumers prove them.
