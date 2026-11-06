// Plugin declarations only; no build logic lives here (see AGENTS.md: empty root project).
// Declaring build-logic plugins `apply false` at root keeps their classes in the shared
// parent scope instead of per-project scopes.
plugins {
    alias(libs.plugins.kotlin.serialization) apply false
}
