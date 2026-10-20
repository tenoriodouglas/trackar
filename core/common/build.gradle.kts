plugins {
    alias(libs.plugins.trackar.android.library)
    alias(libs.plugins.trackar.hilt)
}

android {
    namespace = "app.trackar.core.common"
}

dependencies {
    api(libs.kotlinx.coroutines.android)
}
