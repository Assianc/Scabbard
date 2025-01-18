plugins {
    id("com.android.library")  // 如果 memo 是一个库模块
    kotlin("android")
}

android {
    namespace = "com.assiance.memo"
    compileSdk = 35

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

    // 启用 ViewBinding
    buildFeatures {
        viewBinding = true
    }

    // 正确配置 assets 和 res 目录
    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src/main/assets")
            }
            res {
                srcDirs("src/main/res")
            }
        }
    }
}

dependencies {
    implementation(libs.core.ktx.v1101)
    implementation(libs.appcompat.v161)
    implementation(libs.material.v190)

    // 检查 libs 引用是否正确
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.core.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // 添加 Glide 依赖
    implementation(libs.glide)
    implementation(libs.gson)

    // 添加 lifecycle 相关依赖
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    // 添加 AndroidX interpolator 依赖
    implementation("androidx.interpolator:interpolator:1.0.0")
}
