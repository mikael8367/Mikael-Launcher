pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    versionCatalogs {
        create("libs") {
            from(files("third_party/FoldCraftLauncher/gradle/libs.versions.toml"))
        }
    }
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "MikaelLauncher"
include(":app", ":fcl-native", ":fcl-android", ":Terracotta", ":ZipFileSystem")
project(":Terracotta").projectDir = file("third_party/FoldCraftLauncher/Terracotta")
project(":ZipFileSystem").projectDir = file("third_party/FoldCraftLauncher/ZipFileSystem")
