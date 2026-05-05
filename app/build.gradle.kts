plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ttt.liveassistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ttt.liveassistant"
        minSdk = 26
        targetSdk = 35
        versionCode = 17
        versionName = "0.3.12"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-ads:24.3.0")
}
