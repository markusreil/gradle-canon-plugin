package com.mreil.canon.publish

import com.mreil.canon.semver.SemverService
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

/**
 * Started-not-finished `publish` contributor: registers `publish` and routes via semver service.
 */
class PublishPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (project === project.rootProject) {
            project.allprojects { register(it) }
        } else {
            register(project)
        }
    }

    private fun register(project: Project) {
        val canon =
            project.extensions.findByName("canon") as? ExtensionAware
                ?: error(
                    "com.mreil.canon.publish requires 'canon': apply master (or core) " +
                        "before com.mreil.canon.publish",
                )
        val holder =
            when (val existing = canon.extensions.findByName("publish")) {
                null -> canon.extensions.create("publish", PublishExtension::class.java)
                is PublishExtension -> existing
                else ->
                    error(
                        "canon already has a 'publish' contributor — two contributors claim the same name " +
                            "(existing: ${existing.javaClass.name})",
                    )
            }
        project.afterEvaluate {
            if (holder.enabled.get()) {
                route(project)
            }
        }
    }

    private fun route(project: Project): String =
        if (semverOf(project).isSnapshot()) "snapshot" else "release"

    private fun semverOf(project: Project): SemverService {
        val registration = project.gradle.sharedServices.registrations.getByName("canonSemver")
        return registration.service.get() as SemverService
    }
}
