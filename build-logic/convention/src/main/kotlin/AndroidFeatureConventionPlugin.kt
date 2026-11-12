import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Plantilla de módulo `feature:*`: library + Compose + Hilt + navegación + dependencias de core.
 * Una feature nunca depende de otra feature.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("lifetracker.android.library")
            pluginManager.apply("lifetracker.android.compose")
            pluginManager.apply("lifetracker.android.hilt")
            pluginManager.apply(libs.plugin("kotlin-serialization"))

            dependencies {
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:ui"))
                add("implementation", project(":core:model"))
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:data"))
                add("implementation", project(":core:common"))

                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-navigation-compose"))
                add("implementation", libs.library("androidx-hilt-navigation-compose"))
                add("implementation", libs.library("kotlinx-serialization-json"))
                add("implementation", libs.library("kotlinx-collections-immutable"))

                add("testImplementation", project(":core:testing"))
                add("testImplementation", libs.library("turbine"))
                add("testImplementation", libs.library("kotlinx-coroutines-test"))
            }
        }
}
