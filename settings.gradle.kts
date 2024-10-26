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
        // JitPack 远程仓库
        mavenCentral()
        maven(url = "https://jitpack.io") // 添加 JitPack 仓库
        google()
        mavenCentral()
    }
}

rootProject.name = "Scabbard"
include(":app")
include(":memo")
