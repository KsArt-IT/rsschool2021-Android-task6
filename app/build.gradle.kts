plugins {
    id("com.android.application")
    id("dagger.hilt.android.plugin")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
}

android {
    compileSdk = rootProject.extra["maxSdkVers"] as Int

    defaultConfig {
        applicationId = "ru.ksart.musicapp"
        minSdk = rootProject.extra["minSdkVers"] as Int
        targetSdk = rootProject.extra["maxSdkVers"] as Int
        versionCode = rootProject.extra["codeVers"] as Int
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kapt {
    correctErrorTypes = true
}

apply(from = "${rootDir}/ktlint.gradle")

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.31")
    implementation("androidx.core:core-ktx:1.6.0")
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.1")

    // Fragment
    implementation("androidx.fragment:fragment-ktx:1.3.6")
    implementation("androidx.activity:activity-ktx:1.3.1")
    // ViewModel
    val lifecycle_version = "2.4.0-rc01"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    // Saved State module for ViewModel
//    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycle_version")
    // Lifecycle KTX
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version")
    // Navigation
    val navigation_version = "2.3.5"
    implementation("androidx.navigation:navigation-fragment-ktx:$navigation_version")
    implementation("androidx.navigation:navigation-ui-ktx:$navigation_version")
    // Coroutines and Flow
    val coroutines_version = "1.5.2"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version")
    // Retrofit
    val retrofit_version = "2.9.0"
    implementation("com.squareup.retrofit2:retrofit:$retrofit_version")
    implementation("com.squareup.retrofit2:converter-moshi:$retrofit_version")
    // Okhttp
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.2")
    // Moshi
    val moshi_version = "1.12.0"
    implementation("com.squareup.moshi:moshi:$moshi_version")
    implementation("com.squareup.moshi:moshi-kotlin:$moshi_version")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:$moshi_version")
    // Dagger Hilt
    val hilt_version = "2.39.1"
    implementation("com.google.dagger:hilt-android:$hilt_version")
    kapt("com.google.dagger:hilt-compiler:$hilt_version")
    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    // Coil
//    val coil_version = "1.4.0"
//    implementation("io.coil-kt:coil-base:$coil_version")
//    implementation("io.coil-kt:coil:$coil_version")
    // Glide
    val glide_version = "4.12.0"
    implementation("com.github.bumptech.glide:glide:$glide_version")
    kapt("com.github.bumptech.glide:compiler:$glide_version")
    // Exoplayer
    val exoplayer_version = "2.15.1"
    implementation("com.google.android.exoplayer:exoplayer-core:$exoplayer_version")
    implementation( "com.google.android.exoplayer:extension-mediasession:$exoplayer_version")
    implementation("com.google.android.exoplayer:exoplayer-ui:$exoplayer_version")
    implementation("com.google.android.exoplayer:extension-okhttp:$exoplayer_version")
    //
    implementation("androidx.media:media:1.4.3")

    // Timber
    implementation("com.jakewharton.timber:timber:5.0.1")

    // ViewBindingPropertyDelegate by androidbroadcast
    implementation("com.github.kirich1409:viewbindingpropertydelegate-noreflection:1.5.0-beta01")
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
