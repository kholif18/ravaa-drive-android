plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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
}
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("androidx.room:room-runtime:2.6.1")
}
