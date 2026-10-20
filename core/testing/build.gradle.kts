plugins {
    alias(libs.plugins.trackar.android.library)
}

android {
    namespace = "app.trackar.core.testing"
}

dependencies {
    api(projects.core.data)
    api(projects.core.model)
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
