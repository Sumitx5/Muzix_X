@file:Suppress("DEPRECATION")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.sumit.muzixx"
    //noinspection GradleDependency
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sumit.muzixx"
        minSdk = 24
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 6
        versionName = "1.3.3"

        multiDexEnabled = true

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
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    // Jetpack Compose Ecosystem (Clean references from your new catalog)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Core AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Firebase Ecosystem
    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")

// This one specifically fixes GetGoogleIdTokenOption and GoogleIdTokenCredential!
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Data Storage & Utilities
    //noinspection UseTomlInstead,GradleDependency
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    //noinspection UseTomlInstead,GradleDependency
    implementation("androidx.datastore:datastore-core:1.1.1")
    //noinspection UseTomlInstead
    implementation("androidx.palette:palette-ktx:1.0.0")
    //noinspection UseTomlInstead
    implementation("com.google.code.gson:gson:2.14.0")
    //noinspection UseTomlInstead
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Networking & APIs
    //noinspection UseTomlInstead,GradleDependency
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    //noinspection UseTomlInstead,GradleDependency
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    //noinspection UseTomlInstead,NewerVersionAvailable
    implementation("io.ktor:ktor-client-core:2.3.11")
    //noinspection UseTomlInstead,NewerVersionAvailable
    implementation("io.ktor:ktor-client-okhttp:2.3.11")
    //noinspection UseTomlInstead,NewerVersionAvailable
    implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
    //noinspection UseTomlInstead,NewerVersionAvailable
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")
    //noinspection UseTomlInstead,NewerVersionAvailable
    implementation("com.github.teamnewpipe:NewPipeExtractor:0.26.3")

    // Media & Playback Engine
    val media3Version = "1.4.1"
    //noinspection UseTomlInstead,GradleDependency
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    //noinspection UseTomlInstead,GradleDependency
    implementation("androidx.media3:media3-common:$media3Version")
    //noinspection UseTomlInstead,GradleDependency
    implementation("androidx.media3:media3-session:$media3Version")
    //noinspection GradleDependency,UseTomlInstead
    implementation("androidx.media3:media3-datasource-okhttp:$media3Version")

    //noinspection UseTomlInstead, GradleDependency
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.0.4")

    // Testing Suite
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "com.google.protobuf") {
                useVersion("4.26.1")
            }
        }
    }
}