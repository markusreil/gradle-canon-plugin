package com.mreil.canon.versioning

import com.mreil.canon.versioning.api.SemverService
import com.mreil.canon.versioning.api.parseVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

/** Registers the `versioning` contributor under `canon` on all projects. */
class VersioningPlugin : Plugin<Project> {

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
                    "com.mreil.canon.versioning requires 'canon': apply com.mreil.canon.core " +
                        "(or the canon bundle) before com.mreil.canon.versioning",
                )
        when (val existing = canon.extensions.findByName("versioning")) {
            null -> {
                canon.extensions.create("versioning", VersioningExtension::class.java)
                registerSemver(project)
            }
            is VersioningExtension -> Unit
            else ->
                error(
                    "canon already has a 'versioning' contributor — two contributors claim the same name " +
                        "(existing: ${existing.javaClass.name})",
                )
        }
    }

    private fun registerSemver(project: Project) {
        project.gradle.sharedServices.registerIfAbsent("canonSemver", SemverService::class.java) {
            it.parameters.version.set(project.provider { project.rootProject.version.toString() })
            it.parameters.projectPath.set(project.path)
        }
        project.afterEvaluate {
            parseVersion(project.rootProject.version.toString(), project.path)
        }
    }
}
