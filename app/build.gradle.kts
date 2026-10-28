import java.util.Properties

plugins {
    alias(libs.plugins.trackar.android.application)
    alias(libs.plugins.trackar.android.compose)
    alias(libs.plugins.trackar.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

// Secrets come from local.properties (dev) or environment variables (CI); never committed.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
fun secret(name: String): String? = localProperties.getProperty(name) ?: providers.environmentVariable(name).orNull

// Release signing is enabled only when a keystore is provided (CI deploy job or local.properties).
val releaseKeystore = secret("KEYSTORE_FILE")?.let(::file)?.takeIf { it.exists() }

android {
    namespace = "app.trackar"

    defaultConfig {
        applicationId = "app.trackar"
        // CI sets VERSION_CODE = run number, so every build is unique and increasing.
        versionCode = secret("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["mapsApiKey"] = secret("MAPS_API_KEY").orEmpty()
    }

    signingConfigs {
        // Fixed debug key (committed on purpose): CI runners would otherwise generate a new one per build,
        // and Android refuses to update an app signed with a different key. Grants nothing in production.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = secret("KEYSTORE_PASSWORD")
                keyAlias = secret("KEY_ALIAS")
                keyPassword = secret("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures.buildConfig = true

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.model)
    implementation(projects.core.designsystem)
    implementation(projects.feature.auth)
    implementation(projects.feature.history)
    implementation(projects.feature.map)
    implementation(projects.feature.vehicles)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.compose.material3.navigationSuite)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.core.testing)
}
