plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.scabbard"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.scabbard"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation("com.github.getActivity:XXPermissions:20.0")

    // 基础依赖包，必须要依赖
    implementation("com.geyifeng.immersionbar:immersionbar:3.2.2")

    // AndroidX 基础库
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.5.7")
    // XXPermissions 权限库
    implementation("com.github.getActivity:XXPermissions:20.0")
    // ImmersionBar（状态栏管理）
    implementation("com.geyifeng.immersionbar:immersionbar:3.2.2")
    implementation("com.google.android.material:material:1.9.0")

    implementation(project(":memo"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

