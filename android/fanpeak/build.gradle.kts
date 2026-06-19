plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.fanpeak.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fanpeak.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "DEFAULT_SERVER_URL", "\"http://192.168.31.40:11303\"")  // FanHub backend
    }

    signingConfigs {
        create("release") {
            storeFile = file("fanpeak.keystore")
            storePassword = "fanpeak123"
            keyAlias = "fanpeak"
            keyPassword = "fanpeak123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // APK 输出目录配置 - 复制到固定目录 /mnt/fan/apk
applicationVariants.all {
    outputs.all {
        val fileName = "fanpeak-${name}.apk"
        (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = fileName
    }
}

// 编译完成后自动复制 APK 到固定目录
tasks.whenTaskAdded {
    if (name == "assembleDebug" || name == "assembleRelease") {
        doLast {
            val outputDir = "/mnt/fan/apk"
            copy {
                from(layout.buildDirectory.dir("outputs/apk/${name.replace("assemble", "").lowercase()}"))
                into(outputDir)
            }
            println("FanPeak APK copied to $outputDir")
        }
    }
}
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)  // HLS 流媒体支持
    implementation(libs.media3.exoplayer.dash)  // DASH 流媒体支持
    implementation(libs.media3.exoplayer.rtsp)  // RTSP 支持
    implementation(libs.media3.ui)
    implementation(libs.media3.session)

    // Coil
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Paging
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // Accompanist
    implementation(libs.accompanist.systemuicontroller)

    // Coroutines
    implementation(libs.kotlinx.coroutines)
}
