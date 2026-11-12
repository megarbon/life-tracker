import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.library(alias: String) = findLibrary(alias).get()

internal fun VersionCatalog.plugin(alias: String) = findPlugin(alias).get().get().pluginId

/** Los warnings del compilador rompen el build: la deuda se paga al momento. */
internal fun Project.failOnKotlinWarnings() {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions.allWarningsAsErrors.set(true)
    }
}

/**
 * Avisos de lint que sólo informan de versiones nuevas: las dependencias se actualizan
 * a propósito desde el version catalog, no por un build roto.
 */
internal val LINT_DISABLED_CHECKS = setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")

internal const val COMPILE_SDK = 37
internal const val MIN_SDK = 28
internal const val TARGET_SDK = 37
