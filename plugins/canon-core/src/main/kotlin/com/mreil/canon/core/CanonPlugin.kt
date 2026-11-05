package com.mreil.canon.core

import org.gradle.api.Plugin
import org.gradle.api.Project

class CanonPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (project === project.rootProject) {
            project.allprojects { register(it) }
            CanonPropagation.registerPropagation(project)
        } else {
            register(project)
        }
    }

    private fun register(project: Project) {
        if (project.extensions.findByName("canon") == null) {
            project.extensions.create("canon", CanonExtension::class.java)
        }
    }
}
