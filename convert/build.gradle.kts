import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    // kotlin.android 不能显式声明：AGP 9.0 起 Kotlin 支持已内置，声明了会直接报错。
    // compose 插件不属于内置范围，仍要显式加（与 memo / alm 一致）。
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.assiance.convert"
    compileSdk = 37

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    // ---- Jetpack Compose ----
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.navigation.compose)
    debugImplementation(libs.compose.ui.tooling)

    // 液态玻璃（iOS 26 风格毛玻璃）。convert 的菜单卡与结果卡都用它，
    // 跨模块契约见 ui/ConvertGlassContract.kt。
    implementation(libs.liquid.glass)

    // 农历转换（tyme4kt：纯 Kotlin 多平台），与 alm 日历页同库同版本。
    implementation(libs.tyme4kt)

    // cron 生成逻辑是纯 Kotlin，单测直接跑在 JVM 上，不需要 Robolectric。
    testImplementation(libs.junit)
}
