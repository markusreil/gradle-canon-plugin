package com.mreil.canon.versioning;

import com.mreil.canon.core.CanonPlugin;
import java.io.File;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(SoftAssertionsExtension.class)
class VersioningPluginTest {

    @TempDir
    File projectDir;

    @Test
    void registersCanonAndVersioningOnRootAndChild(SoftAssertions softly) {
        Project root = rootProject("1.2.3");
        Project child = childOf(root, "child");

        root.getPluginManager().apply(CanonPlugin.class);
        root.getPluginManager().apply(VersioningPlugin.class);

        softly.assertThat(root.getExtensions().findByName("canon")).isNotNull();
        softly.assertThat(child.getExtensions().findByName("canon")).isNotNull();
        softly.assertThat(versioningOf(root)).isNotNull();
        softly.assertThat(versioningOf(child)).isNotNull();
    }

    @Test
    void contributorOnlyFailsFastAndRegistersNothing(SoftAssertions softly) {
        Project root = rootProject("1.2.3");

        softly.assertThatThrownBy(() -> root.getPluginManager().apply(VersioningPlugin.class))
                .isInstanceOf(RuntimeException.class)
                .hasStackTraceContaining("requires 'canon'");
        softly.assertThat(root.getExtensions().findByName("canon")).isNull();
    }

    @Test
    void reapplyKeepsExistingInstance(SoftAssertions softly) {
        Project root = rootProject("1.2.3");
        root.getPluginManager().apply(CanonPlugin.class);
        root.getPluginManager().apply(VersioningPlugin.class);
        Object first = versioningOf(root);

        root.getPluginManager().apply(VersioningPlugin.class);

        softly.assertThat(versioningOf(root)).isSameAs(first);
    }

    @Test
    void differentContributorWithSameNameFails(SoftAssertions softly) {
        Project root = rootProject("1.2.3");
        root.getPluginManager().apply(CanonPlugin.class);
        ExtensionAware canon = (ExtensionAware) root.getExtensions().findByName("canon");
        canon.getExtensions().create("versioning", OtherVersioningExtension.class);

        softly.assertThatThrownBy(() -> root.getPluginManager().apply(VersioningPlugin.class))
                .isInstanceOf(RuntimeException.class)
                .hasStackTraceContaining("two contributors claim the same name");
    }

    @Test
    void schemeDefaultsToSemverAndVersionsParse(SoftAssertions softly) {
        Project root = rootProject("1.2.3");
        root.getPluginManager().apply(CanonPlugin.class);
        root.getPluginManager().apply(VersioningPlugin.class);
        VersioningExtension extension = (VersioningExtension) versioningOf(root);

        softly.assertThat(extension.getScheme()).isEqualTo("semver");
        softly.assertThat(extension.parsedVersion().toString()).isEqualTo("1.2.3");
    }

    @Test
    void snapshotVersionParses(SoftAssertions softly) {
        Project root = rootProject("1.2.3-SNAPSHOT");
        root.getPluginManager().apply(CanonPlugin.class);
        root.getPluginManager().apply(VersioningPlugin.class);

        softly.assertThat(
                        ((VersioningExtension) versioningOf(root)).parsedVersion().toString())
                .isEqualTo("1.2.3-SNAPSHOT");
    }

    @Test
    void garbageVersionFailsWithProjectAndValue(SoftAssertions softly) {
        Project root = rootProject("banana");
        root.getPluginManager().apply(CanonPlugin.class);
        root.getPluginManager().apply(VersioningPlugin.class);
        VersioningExtension extension = (VersioningExtension) versioningOf(root);

        softly.assertThatThrownBy(extension::parsedVersion)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("banana");
    }

    @Test
    void missingVersionFailsWithDedicatedMessage(SoftAssertions softly) {
        Project root = rootProject("unspecified");
        root.getPluginManager().apply(CanonPlugin.class);
        root.getPluginManager().apply(VersioningPlugin.class);
        VersioningExtension extension = (VersioningExtension) versioningOf(root);

        softly.assertThatThrownBy(extension::parsedVersion)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has no version set");
    }

    private Object versioningOf(Project project) {
        ExtensionAware canon = (ExtensionAware) project.getExtensions().findByName("canon");
        return canon.getExtensions().findByName("versioning");
    }

    private Project rootProject(String version) {
        Project root = ProjectBuilder.builder().withProjectDir(projectDir).build();
        root.setVersion(version);
        return root;
    }

    private Project childOf(Project root, String name) {
        File dir = new File(projectDir, name);
        if (!dir.mkdirs()) {
            throw new IllegalStateException("Could not create " + dir);
        }
        return ProjectBuilder.builder().withParent(root).withName(name).withProjectDir(dir).build();
    }

    public static class OtherVersioningExtension {}
}
