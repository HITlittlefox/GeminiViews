pluginManagement {
    repositories {
        // 首选 Google 和 Maven Central，官方推荐
        google()
        mavenCentral()

        // 备用：阿里云镜像（加快国内访问速度）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }

        // JitPack - 一些开源项目依赖
        maven { url = uri("https://www.jitpack.io") }

        // 只用于插件构建时
        gradlePluginPortal()
    }
//    repositories {
//        google {
//            content {
//                includeGroupByRegex("com\\.android.*")
//                includeGroupByRegex("com\\.google.*")
//                includeGroupByRegex("androidx.*")
//            }
//        }
//        mavenCentral()
//        gradlePluginPortal()
//    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 首选 Google 和 Maven Central，官方推荐
        google()
        mavenCentral()

        // 备用：阿里云镜像（加快国内访问速度）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }

        // JitPack - 一些开源项目依赖
        maven { url = uri("https://www.jitpack.io") }

        // 只用于插件构建时
        gradlePluginPortal()
    }
}

rootProject.name = "GeminiViews"
include(":app")
 