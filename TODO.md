# TODO

Carried over from the v0.0.1 roadmap when planning restarted: parked ideas and the plugin backlog.
Nothing here is committed or scheduled.

## Later (parked, not forgotten)

- Master packaging: how the bundle ships (leaning fat jar with the curated contributors bundled).
- Publishing/release automation: repository choice, credentials, real release flow (only the local
  test-publish exists so far).
- Real convention logic inside contributors (e.g. what a `jvm` contributor actually configures).
- Smarter test fixtures — explicit strings until duplication hurts.
- Service consumer pattern: fail-fast vs lazy `Provider<Service>`, interface split, fixed `core-api`
  creation/discovery helper — needs a bigger sample size.
- DEEP merge semantics for collection properties (rejected with a clear error until its chunk).

## Plugin backlog

* codemeta: create and maintain a codemeta.json file at the repo root.
* jvm-defaults: toolchain, source/doc jars, test suites configuration
* publish: maven-publish with correct pom coordinates, tiered artifact routing based on release/pre-release
* release: auto release flow: test, tag release version, new snapshot, push (needs git/vcs)
* tbd: per-branch pre-release fragment on produced artifacts to avoid clashes between ci-published artifacts
* changelog: automatic changelog rolling (needs release for trigger)
