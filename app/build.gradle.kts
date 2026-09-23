plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.parcelize")
}
android {
    namespace = "com.ravaa.drive"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.ravaa.drive"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0-HOME"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.10" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    buildTypes {
        debug {
            // Ujicoba via USB: `adb reverse tcp:2713 tcp:2713`, HP akses localhost laptop.
            // WiFi satu subnet: ganti ke http://192.168.40.254:2713/
            // Emulator: ganti ke http://10.0.2.2:2713/
            buildConfigField("String", "DRIVE_BASE_URL", "\"http://127.0.0.1:2713/\"")
        }
        release {
            // TODO: ganti ke domain produksi Ravaa-Drive
            buildConfigField("String", "DRIVE_BASE_URL", "\"https://drive.ravaa.my.id/\"")
        }
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    // Material icons extended (People, Star, Folder, Upload, dsb.)
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // Room + DataStore
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    kapt("androidx.hilt:hilt-compiler:1.1.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    // WorkManager (background sync) + Hilt integration
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0")
    // Pull-to-refresh (Drive-style)
    implementation("com.google.accompanist:accompanist-swiperefresh:0.34.0")
    // Video player (ExoPlayer via Media3)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
}
