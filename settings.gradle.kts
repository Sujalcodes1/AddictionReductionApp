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

rootProject.name = "AddictionReductionApp"
include(":app")
<<<<<<< HEAD

// Redirect build directory to D: drive to avoid disk space, path length, and OneDrive sync issues
gradle.beforeProject {
    val buildDirName = if (project == rootProject) "root" else project.name
    layout.buildDirectory.set(file("D:/gradle_builds/AddictionReductionApp/$buildDirName"))
}
=======
>>>>>>> 64f9bf7574525cae2aab0c9fd49a5ccb21344266
 