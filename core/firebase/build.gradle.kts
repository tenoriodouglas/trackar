plugins {
    alias(libs.plugins.trackar.android.library)
    alias(libs.plugins.trackar.hilt)
}

android {
    namespace = "app.trackar.core.firebase"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
}
