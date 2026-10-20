plugins {
    alias(libs.plugins.trackar.android.feature)
}

android {
    namespace = "app.trackar.feature.map"
}

dependencies {
    implementation(libs.maps.compose)
}
