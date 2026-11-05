package com.mreil.canon.versioning;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(SoftAssertionsExtension.class)
class CanonVersioningFunctionalTest {

    @TempDir
    Path projectDir;

    @Test
    void versioningBlockResolvesOnRootAndChild(SoftAssertions softly) {
        write(
                "settings.gradle.kts",
                """
                rootProject.name = "canon-versioning-l2"
                include("child")
                """);
        write(
                "build.gradle.kts",
                """
                plugins {
                    id("com.mreil.canon.core")
                    id("com.mreil.canon.versioning")
                }

                version = "1.2.3"
                """);
        write(
                "child/build.gradle.kts",
                """
                canon {
                    versioning {
                    }
                }
                """);

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(projectDir.toFile())
                        .withPluginClasspath(pluginClasspath())
                        .withArguments("help")
                        .build();

        softly.assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
    }

    private static List<File> pluginClasspath() {
        try (InputStream in =
                CanonVersioningFunctionalTest.class.getResourceAsStream(
                        "/plugin-under-test-metadata.properties")) {
            Properties metadata = new Properties();
            metadata.load(in);
            List<File> files =
                    Arrays.stream(
                                    metadata.getProperty("implementation-classpath")
                                            .split(File.pathSeparator))
                            .map(File::new).collect(Collectors.toList());
            files.add(new File(System.getProperty("canonCoreJar")));
            return files;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
