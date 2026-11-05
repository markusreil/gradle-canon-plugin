package com.mreil.canon.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(SoftAssertionsExtension.class)
class CanonPluginFunctionalTest {

    @TempDir
    Path projectDir;

    @Test
    void canonBlockResolvesOnRootAndChild(SoftAssertions softly) {
        write(
                "settings.gradle.kts",
                """
                rootProject.name = "canon-l2"
                include("child")
                """);
        write(
                "build.gradle.kts",
                """
                plugins {
                    id("com.mreil.canon.core")
                }

                canon {
                }
                """);
        write(
                "child/build.gradle.kts",
                """
                canon {
                }
                """);

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(projectDir.toFile())
                        .withPluginClasspath()
                        .withArguments("help")
                        .build();

        softly.assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
    }

    private void write(String relativePath, String content) {
        try {
            Path file = projectDir.resolve(relativePath);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
