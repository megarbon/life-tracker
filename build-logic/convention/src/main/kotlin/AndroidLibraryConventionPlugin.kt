import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply(libs.plugin("android-library"))

            extensions.configure<LibraryExtension> {
                compileSdk = COMPILE_SDK
                defaultConfig {
                    minSdk = MIN_SDK
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                lint {
                    warningsAsErrors = true
                    abortOnError = true
                    disable += LINT_DISABLED_CHECKS
                }
            }

            // explicitApi sólo en core: son las APIs que consumen las features.
            if (path.startsWith(":core")) {
                extensions.configure<KotlinBaseExtension> { explicitApi() }
            }
            failOnKotlinWarnings()

            dependencies {
                add("testImplementation", libs.library("junit"))
            }
        }
}
