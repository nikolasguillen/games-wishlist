import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.lib(alias: String): Provider<MinimalExternalModuleDependency> =
    libs.findLibrary(alias).orElseThrow { IllegalStateException("No library '$alias' in libs.versions.toml") }

/** The package a module's code lives in, which is also its Android namespace. */
internal fun Project.questLogNamespace(): String =
    "com.nikolasguillen.questlog." + path.removePrefix(":").split(':').joinToString(".") { it.replace("-", "") }
