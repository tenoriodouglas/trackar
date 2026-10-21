plugins {
    alias(libs.plugins.trackar.android.feature)
}

android {
    namespace = "app.trackar.feature.history"
}

dependencies {
    implementation(libs.maps.compose)
}
