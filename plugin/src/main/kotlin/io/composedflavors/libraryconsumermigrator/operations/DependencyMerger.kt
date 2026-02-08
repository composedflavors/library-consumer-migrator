package io.composedflavors.libraryconsumermigrator.operations


import io.composedflavors.libraryconsumermigrator.parser.TomlParser
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

/**
 * Handles merging of version catalog dependencies from submodule to root project.
 */
class DependencyMerger(
    private val project: Project,
    private val logger: Logger
) {
    private val tomlParser = TomlParser()

    /**
     * Merges dependencies from the submodule's libs.versions.toml to the root catalog.
     * Cleans up the submodule's version catalog after merging.
     */
    fun mergeDependencies(submoduleName: String) {
        logger.lifecycle("Merging dependencies to root libs.versions.toml...")

        val exampleLibsFile = project.rootDir.resolve("$submoduleName/gradle/libs.versions.toml")
        val rootLibsFile = project.rootDir.resolve("gradle/libs.versions.toml")

        if (!exampleLibsFile.exists() || !rootLibsFile.exists()) {
            logger.lifecycle("  Version catalog not found, skipping merge")
            return
        }

        val mergeResult = tomlParser.mergeCatalogs(
            sourceFile = exampleLibsFile,
            targetFile = rootLibsFile,
            sourceName = submoduleName
        )

        if (mergeResult.hasChanges) {
            logger.lifecycle("  Added ${mergeResult.addedCount} new dependencies")
            logAddedDependencies(mergeResult)
        } else {
            logger.lifecycle("  No new dependencies to merge")
        }

        cleanupVersionCatalog(exampleLibsFile)
    }

    private fun logAddedDependencies(mergeResult: TomlParser.MergeResult) {
        if (mergeResult.addedBySection.isNotEmpty()) {
            logger.info("  Breakdown by section:")
            mergeResult.addedBySection.forEach { (section, count) ->
                logger.info("    - $section: $count")
            }
        }
    }

    private fun cleanupVersionCatalog(file: File) {
        file.delete()
        logger.debug("  Deleted submodule's version catalog")

        file.parentFile?.let { gradleDir ->
            if (gradleDir.exists() && gradleDir.listFiles()?.isEmpty() == true) {
                gradleDir.delete()
                logger.debug("  Deleted empty gradle directory")
            }
        }
    }
}