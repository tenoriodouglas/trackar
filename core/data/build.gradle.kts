plugins {
    alias(libs.plugins.trackar.android.library)
    alias(libs.plugins.trackar.hilt)
}

android {
    namespace = "app.trackar.core.data"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.firebase)
}
