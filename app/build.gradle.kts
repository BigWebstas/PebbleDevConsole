plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "net.webstas.pebbledev"
    compileSdk = 37

    defaultConfig {
        applicationId = "net.webstas.pebbledev"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx-android:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose-android:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-android:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose-android:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate-android:2.11.0")

    implementation("androidx.compose.runtime:runtime-android:1.13.0-alpha03")
    implementation("androidx.compose.ui:ui-android:1.12.1")
    implementation("androidx.compose.ui:ui-graphics-android:1.12.1")
    implementation("androidx.compose.ui:ui-text-android:1.12.1")
    implementation("androidx.compose.foundation:foundation-android:1.12.1")
    implementation("androidx.compose.foundation:foundation-layout-android:1.12.1")
    implementation("androidx.compose.animation:animation-android:1.13.0-alpha03")
    implementation("androidx.compose.material:material-ripple-android:1.12.1")
    implementation("androidx.compose.material3:material3-android:1.5.0-beta01") {
        exclude(group = "androidx.compose.material", module = "material-ripple")
    }
    implementation("androidx.compose.material3:material3-ripple-android:1.5.0-beta01")
    implementation("androidx.compose.material:material-icons-core-android:1.7.8")
    implementation("androidx.compose.material:material-icons-extended-android:1.7.8")

    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20260814")
}
