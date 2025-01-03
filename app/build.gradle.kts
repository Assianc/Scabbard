plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.scabbard"
    compileSdk = 34


    defaultConfig {
        applicationId = "com.example.scabbard"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "3.4.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
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

    kotlinOptions {
        jvmTarget = "11"
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation("com.github.getActivity:XXPermissions:20.0")

    // 基础依赖包，必须要依赖
    implementation("com.geyifeng.immersionbar:immersionbar:3.2.2")

    // AndroidX 基础库
    implementation("androidx.fragment:fragment-ktx:1.8.4")

    // ImmersionBar（状态栏管理）
    implementation("com.geyifeng.immersionbar:immersionbar:3.2.2")

    // 本地模块依赖
    implementation(project(":memo"))
    implementation(project(":alm"))

    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
