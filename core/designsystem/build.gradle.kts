plugins {
    alias(libs.plugins.trackar.android.library)
    alias(libs.plugins.trackar.android.compose)
}

android {
    namespace = "app.trackar.core.designsystem"
}

dependencies {
    api(projects.core.model)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.iconsExtended)
    api(libs.androidx.compose.ui)
}
