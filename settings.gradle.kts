rootProject.name = "gradle-easy-plugin-2"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

include("plugins:canon-core")
include("plugins:canon-versioning")
