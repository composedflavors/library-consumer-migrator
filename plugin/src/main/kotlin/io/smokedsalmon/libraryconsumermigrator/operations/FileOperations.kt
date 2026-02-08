package io.smokedsalmon.libraryconsumermigrator.operations

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

/**
 * Handles file system operations for cleaning up standalone project files.
 */
class FileOperations(
    private val project: Project,
    private val logger: Logger
) {

    /**
     * Removes standalone project files and directories from the submodule.
     * These files are not needed when the app is part of a larger project.
     */
    fun removeStandaloneFiles(submoduleName: String) {
        logger.lifecycle("Removing standalone project files...")

        val submoduleDir = project.rootDir.resolve(submoduleName)
        var removedCount = 0

        STANDALONE_FILES.forEach { filename ->
            val file = submoduleDir.resolve(filename)
            if (file.exists()) {
                file.delete()
                logger.info("  Removed file: $filename")
                removedCount++
            }
        }

        STANDALONE_DIRECTORIES.forEach { dirName ->
            val dir = submoduleDir.resolve(dirName)
            if (dir.exists()) {
                dir.deleteRecursively()
                logger.info("  Removed directory: $dirName")
                removedCount++
            }
        }

        logger.lifecycle("  Removed $removedCount standalone items")
    }

    companion object {
        private val STANDALONE_FILES = listOf(
            "build.gradle.kts",
            "settings.gradle",
            "settings.gradle.kts",
            "gradlew",
            "gradlew.bat",
            "gradle.properties",
            "local.properties",
            ".gitignore"
        )

        private val STANDALONE_DIRECTORIES = listOf(
            "gradle/wrapper",
            ".gradle"
        )
    }
}