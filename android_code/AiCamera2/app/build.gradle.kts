plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // ========== 必须加：Compose Compiler Gradle 插件 ==========
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    // 序列化插件
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

android {
    namespace = "com.example.aicamera"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.aicamera"
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    // ========== 核心：启用Compose（必须加，否则Compose代码标红） ==========
    buildFeatures {
        compose = true // 开启Compose支持
        viewBinding = true // 保留ViewBinding（兼容CameraX的PreviewView）
    }
    // ========== Compose编译器配置（必须匹配版本） ==========
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get() // 从toml读取版本
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildToolsVersion = "36.1.0"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Lifecycle & ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.camera.view)
    implementation(libs.androidx.compose.animation.core)

    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.12.1")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // 序列化依赖
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // 网络请求
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    // SDK 内部可能用到的基础库
    implementation("com.google.code.gson:gson:2.10.1")

    // 引用星火语音听写arr文件
    implementation(files("libs/SparkChain.aar"))
    implementation(files("libs/Codec.aar"))
}