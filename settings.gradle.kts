pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "mesh_base_libraries"
include(":app")
include(":mesh_base_core")
include(":mesh_base_android")
include(":mesh_base_flutter")
project(":mesh_base_flutter").projectDir = file("mesh_base_flutter/android")
