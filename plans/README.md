# Plans tracker

Single view of what's done and what's still todo. Updated whenever a chunk starts or lands
(see `AGENTS.md` §7). Detailed per-chunk plans live next to this file as `NN-<slug>.md`.

| # | Chunk | Plan file | Status | Notes |
|---|-------|-----------|--------|-------|
| 0 | High-level plan (this roadmap) | `00-high-level-plan.md` | done | Approved; detailed plans follow one chunk at a time |
| 1 | Build scaffolding + test infrastructure | `01-scaffolding-and-test-infra.md` | done | Gate green; retrospective held with no changes |
| 2 | Core `easy` extension plugin | `02-core-easy-extension.md` | in progress | Plan in review; no code until approved |
| 3 | Contributor contract (`easy.jvm` reference) | `03-contributor-contract.md` | todo | — |
| 4 | Root → child propagation (deep/shallow/immutable) | `04-extension-propagation.md` | todo | — |
| 5 | Master bundling plugin (classpath auto-apply) | `05-master-plugin.md` | todo | — |
| 6 | Third-party contributor interop | `06-third-party-interop.md` | todo | — |
| 7 | Shared build-service APIs (separate artifacts) | `07-shared-build-services.md` | todo | — |
| 8 | Published-artifact linking verification | `08-linking-verification.md` | todo | Depends on test-publish infra from Chunk 1 |

Statuses: `todo` → `in progress` (plan approved, work started) → `done` (acceptance criteria + verification met).
