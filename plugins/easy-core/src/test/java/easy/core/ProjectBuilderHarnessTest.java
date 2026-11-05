package easy.core;

import java.nio.file.Path;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(SoftAssertionsExtension.class)
class ProjectBuilderHarnessTest {

    @Test
    void builderCreatesProjectInGivenDir(@TempDir Path projectDir, SoftAssertions softly) {
        Project project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build();

        softly.assertThat(project.getLayout().getProjectDirectory().getAsFile())
                .isEqualTo(projectDir.toFile());
        softly.assertThat(
                        project.getLayout()
                                .getBuildDirectory()
                                .getAsFile()
                                .get()
                                .getAbsolutePath())
                .startsWith(projectDir.toFile().getAbsolutePath());
    }
}
