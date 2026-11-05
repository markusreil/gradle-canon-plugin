# canon-thirdparty-sample

Test fixture proving a non-bundled contributor interoperates with the master bundle. It is not a
shipped artifact story, and it never joins master's dependencies.

## Role

- Applies alongside `com.mreil.canon` in test builds and wires into the same `canon` tree as `extra`.
- Depends only on `canon-api` (plus Gradle API) — the exact classpath a real third party sees. This
  proves the contributor contract needs no core classes.
- Exercises the non-immutable path: `mode` is a `Property<String>` (`auto` convention) with an
  explicit `@Propagate(SHALLOW)` mark.
- Its test suite asserts what the contract promises: coexistence with bundled contributors,
  root → child propagation of foreign-holder values, both apply orders, and the fail-fast message
  on name clashes.
