pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MindSparQ-Jarvis"
include(":app")
include(":core:model")
include(":core:database")
include(":core:policy")
include(":core:designsystem")
include(":engine:loop")
include(":engine:test")
include(":ai:skills")
include(":ai:gateway")
include(":ai:mcp")
include(":engine:index")
include(":engine:graph")
include(":engine:memory")
