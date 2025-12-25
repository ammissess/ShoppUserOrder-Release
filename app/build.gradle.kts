plugins {
    id("com.android.application")
    kotlin("android")

    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    kotlin("kapt")
}

android {
    namespace = "com.example.deliveryapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.deliveryapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue("string", "MAPBOX_ACCESS_TOKEN", "\"YOUR_MAPBOX_ACCESS_TOKEN\"")
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8" // Downgrade nhẹ để tương thích BOM cũ
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

hilt {
    enableAggregatingTask = true
}

dependencies {

    //lifecycle trong compose
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose BOM (downgrade về 2024.09.00 để tương thích AGP 8.3.2)
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    implementation(libs.androidx.lifecycle.runtime.compose.android)
    implementation(libs.androidx.appcompat.resources)
    //kapt("com.google.dagger:hilt-compiler:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")

    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Mapbox
    implementation("com.mapbox.maps:android-ndk27:11.15.0")
    implementation("com.mapbox.extension:maps-compose-ndk27:11.15.0")

    //nut fix GpsFixed
    implementation("androidx.compose.material:material-icons-extended")

    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Javapoet
    implementation("com.squareup:javapoet:1.13.0")

    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Coroutines support for Play Services (nếu chưa có)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

// Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.09.01"))  // SỬA: Đồng bộ BOM cho test
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

