import app.trackar.buildlogic.library
import app.trackar.buildlogic.libs
import app.trackar.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Feature module = Android library + Compose + Hilt + standard UI stack. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply {
            apply(libs.pluginId("trackar-android-library"))
            apply(libs.pluginId("trackar-android-compose"))
            apply(libs.pluginId("trackar-hilt"))
            apply(libs.pluginId("kotlin-serialization"))
        }

        dependencies {
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:data"))
            add("implementation", project(":core:model"))
            add("implementation", project(":core:common"))

            add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-navigation3-runtime"))
            add("implementation", libs.library("kotlinx-serialization-json"))

            add("testImplementation", project(":core:testing"))
        }
    }
}
