import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.assiance.scabbard"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.assiance.scabbard"
        minSdk = 26
        targetSdk = 35
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

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                // 获取版本名
                val versionName = variant.versionName
                // 设置输出APK文件名
                output.outputFileName = "Scabbard-${versionName}.apk"
            }
    }
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
