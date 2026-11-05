package com.mreil.canon.core

import java.io.File
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir

@ExtendWith(SoftAssertionsExtension::class)
class ProjectBuilderHarnessTest {

    @TempDir lateinit var projectDir: File

    @Test
    fun builderCreatesProjectInGivenDir(softly: SoftAssertions) {
        val project: Project = ProjectBuilder.builder().withProjectDir(projectDir).build()

        softly.assertThat(project.layout.projectDirectory.asFile).isEqualTo(projectDir)
        softly
            .assertThat(project.layout.buildDirectory.get().asFile.absolutePath)
            .startsWith(projectDir.absolutePath)
    }
}
