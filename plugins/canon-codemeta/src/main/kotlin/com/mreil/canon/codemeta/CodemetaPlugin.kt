package com.mreil.canon.codemeta

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

/** `codemeta` contributor: registers the holder, the shared service, and the init task. */
class CodemetaPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (project === project.rootProject) {
            project.allprojects { register(it) }
            val init = project.tasks.register("initCodemeta", InitCodemetaTask::class.java)
            init.configure {
                it.doLast("notify") {
                    if ((it as InitCodemetaTask).created) {
                        throw GradleException(
                            "Created 'codemeta.json': it holds this project's machine-readable metadata " +
                                "(name, authors, license, links; see https://codemeta.github.io/). " +
                                "Fill in the TODO placeholders, then rerun the build",
                        )
                    }
                }
            }
            if (!initRequested(project)) {
                project.allprojects { sub ->
                    sub.tasks.configureEach {
                        if (it.name != "initCodemeta") it.dependsOn(init)
                    }
                }
            }
        } else {
            register(project)
        }
    }

    private fun initRequested(project: Project): Boolean =
        project.gradle.startParameter.taskNames.any {
            it.substringAfterLast(':') == "initCodemeta"
        }

    private fun register(project: Project) {
        val canon =
            project.extensions.findByName("canon") as? ExtensionAware
                ?: error(
                    "com.mreil.canon.codemeta requires 'canon': apply master (or core) " +
                        "before com.mreil.canon.codemeta",
                )
        when (val existing = canon.extensions.findByName("codemeta")) {
            null -> {
                canon.extensions.create("codemeta", CodemetaExtension::class.java)
                registerService(project)
            }
            is CodemetaExtension -> Unit
            else ->
                error(
                    "canon already has a 'codemeta' contributor — two contributors claim the same name " +
                        "(existing: ${existing.javaClass.name})",
                )
        }
    }

    private fun registerService(project: Project) {
        project.gradle.sharedServices.registerIfAbsent(
            "canonCodemeta",
            CodemetaService::class.java,
        ) {
            it.parameters.rootDir.set(project.rootProject.layout.projectDirectory)
        }
    }
}
