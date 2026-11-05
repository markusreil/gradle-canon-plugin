package com.mreil.canon.master

import com.mreil.canon.core.CanonPlugin
import com.mreil.canon.versioning.VersioningPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Bundle entry point: applies core plus every curated contributor to the project. */
class CanonMasterPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply(CanonPlugin::class.java)
        project.pluginManager.apply(VersioningPlugin::class.java)
    }
}
