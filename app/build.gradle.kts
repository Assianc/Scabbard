plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.assiance.scabbard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.assiance.scabbard"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "4.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xopt-in=kotlinx.coroutines.FlowPreview"
        )
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

dependencies {
    implementation(libs.core.ktx.v1150)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.getactivity.xxpermissions)

    // 基础依赖包，必须要依赖
    implementation(libs.geyifeng.immersionbar)

    // AndroidX 基础库
    implementation(libs.fragment.ktx)

    // ImmersionBar（状态栏管理）
    implementation(libs.geyifeng.immersionbar)

    // 本地模块依赖
    implementation(project(":memo"))
    implementation(project(":alm"))

    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // 添加以下依赖
    implementation(libs.lifecycle.runtime.ktx)

}
