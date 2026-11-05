# goal

Create a plugin system for gradle that implements conventions for different parts of the build lifecycle.
I've tried this before but I'd like to start over.

# details

* one core plugin that provides an extension container that is automatically registered on all projects ("easy")
* contributor plugins are separate plugins and can create sub-extensions (e.g. "easy.jvm") on the core extension.
* one standalone "master" plugin that can choose a set of contributors to be bundled together with the core plugin, ideally
  they will be auto-applied just because they are on the master plugins classpath.
* non-standard or 3rd party contributor plugins can be applied in the build file alongside the master plugin and will wire themselves in correctly.
* the complete extension tree will be copied from the root project into the children. copies can be deep, shallow, immutable depending
  on whether it is makes sense for specific value to be extended, overrwritten in a child project.
* contributor sub-extensions expose public state as Gradle lazy property types (`Property`, `ListProperty`,
  …) — diverting from that rule needs a good reason (propagation and convention-mapping only understand
  these types). Extensions never take `Project` as a constructor `val`; inject `ObjectFactory` instead
  and let the plugin wire the project. Holders stay concrete (`@Inject` constructor + concrete
  `val`s): `abstract` holders have no backing fields for field-targeted `@Propagate` and no
  name-matchable fields for the reflection copy pass.
* Contributors can define public apis as shared build services (must be separate build artifacts) that can be shared and then used by other
  contributors.

# gradle project

* empty root project
* libs version catalog
* jvm test suites
* junit 5 and assertj soft assertions
* no external plugins unless explicitly allowed

# testing

* prioritize unit test coverage with ProjectBuilder
* functional tests for higher-level interaction using GradleRunner
* what's a good way to write/consume test project files consumed by GradleRunner? lean to: files created by explicit strings at test runtime, introduce smarter fixtures olong the way when needed / the opportunity arises
* tests that verify the top-level linking functionality need to be a seaparate project, pulling in dependencies as if they were already published to a maven repo. how do we integrate that in the build process. suggestion: define a new type of test suite that runs after a test-publish to a local repo or similar.

# workflow

* Start each feature with an implementation plan. Prefer small deliverables over a huge change. Implement one-by-one.
* Always, spec tests first. No implementation without a test that covers it
* For high-level tests (above unit) let's try to stick to Dave Farley's four-layer model for acceptance testing. Let's think carefully about L2, the dsl. This ties in with the "test project files" problem mentioned before.

