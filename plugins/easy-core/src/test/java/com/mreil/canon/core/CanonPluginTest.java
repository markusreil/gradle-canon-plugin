package com.mreil.canon.core;

import java.io.File;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(SoftAssertionsExtension.class)
class CanonPluginTest {

    @TempDir
    File projectDir;

    @Test
    void registersCanonOnRootAndChild(SoftAssertions softly) {
        Project root = rootProject();
        Project child = childOf(root, "child");

        root.getPluginManager().apply(CanonPlugin.class);

        softly.assertThat(root.getExtensions().findByName("canon"))
                .isInstanceOf(CanonExtension.class);
        softly.assertThat(child.getExtensions().findByName("canon"))
                .isInstanceOf(CanonExtension.class);
    }

    @Test
    void reapplyIsHarmless(SoftAssertions softly) {
        Project root = rootProject();
        root.getPluginManager().apply(CanonPlugin.class);
        Object first = root.getExtensions().findByName("canon");

        root.getPluginManager().apply(CanonPlugin.class);

        softly.assertThat(root.getExtensions().findByName("canon")).isSameAs(first);
    }

    @Test
    void directApplicationRegistersLocallyOnly(SoftAssertions softly) {
        Project root = rootProject();
        Project child = childOf(root, "child");

        child.getPluginManager().apply(CanonPlugin.class);

        softly.assertThat(child.getExtensions().findByName("canon"))
                .isInstanceOf(CanonExtension.class);
        softly.assertThat(root.getExtensions().findByName("canon")).isNull();
    }

    private Project rootProject() {
        return ProjectBuilder.builder().withProjectDir(projectDir).build();
    }

    private Project childOf(Project root, String name) {
        File dir = new File(projectDir, name);
        if (!dir.mkdirs()) {
            throw new IllegalStateException("Could not create " + dir);
        }
        return ProjectBuilder.builder().withParent(root).withName(name).withProjectDir(dir).build();
    }
}
