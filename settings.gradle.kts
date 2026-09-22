pluginManagement {
    repositories {
        // 阿里云镜像（国内直连，放最前）
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
        maven(url = "https://maven.aliyun.com/repository/public")
        maven(url = "https://maven.aliyun.com/repository/google")
        // 官方源兜底
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
        // 阿里云镜像（国内直连，放最前）
        maven(url = "https://maven.aliyun.com/repository/public") // Maven Central + JCenter
        maven(url = "https://maven.aliyun.com/repository/google") // 对应 google()
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
        // JitPack 远程仓库（阿里云无代理，走原站；XXPermissions 等依赖在此）
        maven(url = "https://jitpack.io")
        // 官方源兜底
        mavenCentral()
        google()
    }
}

rootProject.name = "Scabbard"
include(":app")
include(":memo")
include(":alm")
