rootProject.name = "gradle-easy-plugin-2"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

include("plugins:canon-api")
include("plugins:canon-core")
include("plugins:canon-versioning")
include("plugins:canon-master")
include("plugins:canon-publish")
include("plugins:canon-versioning-api")
include("plugins:canon-thirdparty-sample")
include("linking")
