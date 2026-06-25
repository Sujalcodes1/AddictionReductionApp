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

// Redirect build directory outside of OneDrive to fix AccessDeniedException and file locking issues
gradle.beforeProject {
    val buildDirName = if (project == rootProject) "root" else project.name
    layout.buildDirectory.set(file("C:/gradle_builds/AddictionReductionApp/$buildDirName"))
}
