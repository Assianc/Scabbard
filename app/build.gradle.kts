import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    // kotlin.android 不能再显式声明：AGP 9.0 起 Kotlin 支持已内置，再声明会直接报
    // "The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin
    // support since AGP 9.0"。compose 插件不属于内置范围，仍要显式加。
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.assiance.scabbard"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.assiance.scabbard"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "4.2.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        // viewBinding 已关掉：app 模块的 XML 布局随界面迁移全部删除了，没有可生成的绑定类。
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

// 输出 APK 改名。
//
// AGP 9 把旧的 Variant API 整个移除了：`applicationVariants` 连同它的实现类
// `BaseVariantOutputImpl` 一起没了；而新的公开 API（`VariantOutput`）**没有**
// `outputFileName` —— 也就是说"在配置阶段直接指定产物文件名"这条路已经封死。
//
// 所以改成产物生成之后统一改名，做法与 LoveScore（同样跑在 AGP 9.0.1 上）一致：
// 独立 Task 类 + `finalizedBy`，两者都是为了配置缓存能正确命中。
//
// 与改造前对齐的点：debug / release **两个产物都改**（原来的 `applicationVariants.all`
// 也是两个都覆盖），名字仍是 `Scabbard-<版本名>.apk`。
// Android Studio 的 Run 走的是 `packageDebug` 的输出路径而不是文件名，不受影响。
abstract class RenameApkTask : DefaultTask() {

    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val apkDirs: ListProperty<String>

    @TaskAction
    fun rename() {
        val target = "Scabbard-${versionName.get()}.apk"
        for (path in apkDirs.get()) {
            val dir = File(path)
            if (!dir.isDirectory) continue
            dir.listFiles()?.forEach { apk ->
                if (apk.isFile && apk.name.endsWith(".apk") && apk.name != target) {
                    val renamed = File(dir, target)
                    if (renamed.exists()) renamed.delete()
                    apk.renameTo(renamed)
                }
            }
        }
    }
}

val renameApks = tasks.register<RenameApkTask>("renameApks") {
    versionName.set(android.defaultConfig.versionName ?: "unknown")
    apkDirs.set(
        listOf("debug", "release").map { variant ->
            layout.buildDirectory.dir("outputs/apk/$variant").get().asFile.path
        },
    )
}

// AGP 注册好 assemble* 之后再挂 —— 直接在 androidComponents 里写会在任务图还没建好时求值。
afterEvaluate {
    tasks.matching { it.name == "assembleDebug" || it.name == "assembleRelease" }
        .configureEach { finalizedBy(renameApks) }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        // 迁移自 android.kotlinOptions.freeCompilerArgs 里那三条 "-Xopt-in="：
        // 该 flag 在 Kotlin 2.x 已更名为 -opt-in，这里直接用类型化的 optIn 列表表达。
        optIn.addAll(
            "kotlin.RequiresOptIn",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.coroutines.FlowPreview",
        )
    }
}

dependencies {
    // 界面全部搬到 Compose 之后，app 模块自己的代码里已经没有任何 AppCompat / Fragment /
    // ConstraintLayout / Navigation-fragment / ImmersionBar 的引用了（逐个 grep 核过），
    // 这些依赖连同它们的重复声明一并删掉。
    // material 保留：XML 主题 Theme.Scabbard 继承的 Theme.Material3.* 由它提供，
    // 那一帧"Compose 还没画出来时的窗口底色"要靠它。appcompat 由 material 传递进来。
    implementation(libs.core.ktx.v1150)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.getactivity.xxpermissions)
    implementation(libs.lifecycle.runtime.ktx)

    // 本地模块依赖 —— 主模块 → 辅助模块，方向不变
    implementation(project(":memo"))
    implementation(project(":alm"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // ---- Jetpack Compose ----
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // 液态玻璃（iOS 26 风格毛玻璃）。本次只做"可解析 + 可编译"验证，尚无任何界面调用它。
    implementation(libs.liquid.glass)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
