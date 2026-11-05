package com.mreil.canon.versioning

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

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
            null -> canon.extensions.create("versioning", VersioningExtension::class.java, project)
            is VersioningExtension -> Unit
            else ->
                error(
                    "canon already has a 'versioning' contributor — two contributors claim the same name " +
                        "(existing: ${existing.javaClass.name})",
                )
        }
    }
}
