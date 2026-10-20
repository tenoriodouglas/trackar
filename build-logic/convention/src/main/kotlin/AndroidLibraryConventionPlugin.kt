import app.trackar.buildlogic.configureKotlinAndroid
import app.trackar.buildlogic.library
import app.trackar.buildlogic.libs
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            defaultConfig.consumerProguardFiles("consumer-rules.pro")
            testOptions.animationsDisabled = true
            // Unique resource prefix per module avoids clashes.
            resourcePrefix = path.split("""\W""".toRegex()).drop(1).distinct()
                .joinToString(separator = "_").lowercase() + "_"
        }

        dependencies {
            add("testImplementation", libs.library("junit"))
            add("testImplementation", libs.library("kotlinx-coroutines-test"))
            add("testImplementation", libs.library("turbine"))
        }
    }
}
