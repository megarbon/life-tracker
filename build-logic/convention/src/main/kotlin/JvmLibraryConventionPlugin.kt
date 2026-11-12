import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** Módulo Kotlin puro (sin Android), p. ej. `core:model`. `explicitApi` activado. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply(libs.plugin("kotlin-jvm"))

            // Java y Kotlin deben compartir target; sin esto Java usa el JDK que ejecuta Gradle.
            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            extensions.configure<KotlinJvmProjectExtension> {
                explicitApi = ExplicitApiMode.Strict
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
            failOnKotlinWarnings()

            dependencies {
                add("testImplementation", libs.library("junit"))
            }
        }
}
