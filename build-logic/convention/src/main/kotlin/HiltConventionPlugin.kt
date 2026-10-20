import app.trackar.buildlogic.library
import app.trackar.buildlogic.libs
import app.trackar.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("ksp"))

        dependencies {
            add("ksp", libs.library("hilt-compiler"))
        }

        // Pure JVM modules only need the annotations; Android modules need the full Gradle plugin.
        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            dependencies { add("implementation", libs.library("hilt-core")) }
        }
        pluginManager.withPlugin("com.android.base") {
            pluginManager.apply(libs.pluginId("hilt"))
            dependencies { add("implementation", libs.library("hilt-android")) }
        }
    }
}
