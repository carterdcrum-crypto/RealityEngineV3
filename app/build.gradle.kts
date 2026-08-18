plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.realityengine.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.realityengine.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.1-bootstrap"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

// Intentionally dependency-light bootstrap build. Restore feature dependencies after launch is proven.
