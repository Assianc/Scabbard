import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
// 所以改成产物生成之后再产出一份改名副本。debug / release 各注册一个任务
// （与改造前对齐：原来 `applicationVariants.all` 也是两个都覆盖），
// 名字仍是 `Scabbard-<版本名>.apk`。
//
// **为什么是「复制」而不是「改名」**（2026-09-24 修）：
// 最初的版本直接在 `build/outputs/apk/<variant>/` 里把 `app-debug.apk` 改名成
// `Scabbard-<版本>.apk`。这等于把 `packageDebug` 的产物从 Gradle 的追踪里挪走 ——
// 它下次执行时看到自己的输出不见了，会走异常路径。实测出现过一次 `:app:assembleDebug`
// 产出 **41 KB 的残包**（解包只有一个 `classes5.dex`），`adb install` 报
// `INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION: Failed to parse ... AndroidManifest.xml`
// —— **那个 manifest 报错是假象**，manifest 本身完全正常，是包残缺（别顺着 manifest 查）。
// 改成复制后 `packageDebug` 的产物原地不动，两边互不干扰。
//
// 代价是 `build/outputs/apk/debug/` 下会同时存在 `app-debug.apk` 与改名副本。
// 这是有意接受的：Android Studio 的 Run 走的就是 `packageDebug` 的输出路径，
// 留着它反而更贴合 AS 的预期。
//
// 另外，`@InputFile` / `@OutputFile` 是必须的：原版只声明了 `@Input`、没有任何
// `@Output*`，Gradle 无从判断新鲜度，等于每次 assemble 都必然重跑。
abstract class RenameApkTask : DefaultTask() {

    /** `packageDebug` / `packageRelease` 产出的原始 APK（AGP 的默认名 `app-<variant>.apk`）。 */
    @get:InputFile
    abstract val sourceApk: RegularFileProperty

    /** 改名后的副本。声明成输出，Gradle 才会追踪它的新鲜度。 */
    @get:OutputFile
    abstract val renamedApk: RegularFileProperty

    @TaskAction
    fun copyRenamed() {
        sourceApk.get().asFile.copyTo(renamedApk.get().asFile, overwrite = true)
    }
}

/** 给单个 variant 注册改名任务。`variant` 取 `debug` / `release`。 */
fun registerRenameApkTask(variant: String): TaskProvider<RenameApkTask> {
    val renamedFileName = "Scabbard-${android.defaultConfig.versionName ?: "unknown"}.apk"
    return tasks.register<RenameApkTask>(
        "rename${variant.replaceFirstChar { it.uppercaseChar() }}Apk",
    ) {
        val apkDir = layout.buildDirectory.dir("outputs/apk/$variant")
        sourceApk.set(apkDir.map { it.file("app-$variant.apk") })
        renamedApk.set(apkDir.map { it.file(renamedFileName) })
    }
}

// AGP 注册好 assemble* 之后再挂 —— 直接在 androidComponents 里写会在任务图还没建好时求值。
afterEvaluate {
    val renameDebugApk = registerRenameApkTask("debug")
    val renameReleaseApk = registerRenameApkTask("release")
    tasks.matching { it.name == "assembleDebug" }.configureEach { finalizedBy(renameDebugApk) }
    tasks.matching { it.name == "assembleRelease" }.configureEach { finalizedBy(renameReleaseApk) }
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
    implementation(project(":convert"))

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
