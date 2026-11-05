package easy.core;

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
class GradleRunnerHarnessTest {

    @TempDir
    Path projectDir;

    @Test
    void helpSucceedsOnEmptyBuild(SoftAssertions softly) {
        write(
                "settings.gradle.kts",
                """
                rootProject.name = "harness"
                """);
        write(
                "build.gradle.kts",
                """
                // empty harness build
                """);

        BuildResult result =
                GradleRunner.create()
                        .withProjectDir(projectDir.toFile())
                        .withPluginClasspath()
                        .withArguments("help")
                        .build();

        softly.assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
    }

    private void write(String fileName, String content) {
        try {
            Files.writeString(projectDir.resolve(fileName), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
