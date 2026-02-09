package io.github.yuriikyry4enko.libraryconsumermigrator.operations

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

/**
 * Updates project configuration files to integrate the submodule.
 */
class ProjectUpdater(
    private val project: Project,
    private val logger: Logger
) {

    fun updateProjectFiles(submoduleName: String) {
        updateExampleBuildGradle(submoduleName)
        updateSettingsGradle(submoduleName)
        updateRootBuildGradle()
    }

    private fun updateExampleBuildGradle(submoduleName: String) {
        logger.lifecycle("Updating example's build.gradle.kts...")

        val submoduleDir = project.rootDir.resolve(submoduleName)

        val hasBuildFile = submoduleDir.resolve("build.gradle.kts").exists() ||
                submoduleDir.listFiles()
                    ?.any { it.isDirectory && it.resolve("build.gradle.kts").exists() }
                ?: false

        if (!hasBuildFile) {
            logger.warn("  build.gradle.kts not found in submodule")
            return
        }

        logger.lifecycle("  Example project will use root's version catalog")
    }

    private fun updateSettingsGradle(submoduleName: String) {
        logger.lifecycle("Updating settings.gradle.kts...")

        val settingsFile = findSettingsFile()
            ?: throw IllegalStateException("settings.gradle.kts not found in root directory")

        var content = settingsFile.readText()
        val submoduleDir = project.rootDir.resolve(submoduleName)

        val modulesToInclude = findModulesToInclude(submoduleDir, submoduleName)

        if (modulesToInclude.isEmpty()) {
            logger.warn("  No modules with build.gradle.kts found in $submoduleName")
            return
        }

        var addedCount = 0
        modulesToInclude.forEach { modulePath ->
            if (!content.contains(modulePath)) {
                val includeStatement = "include(\"$modulePath\")"
                settingsFile.appendText("\n$includeStatement")
                logger.lifecycle("  Added module: $modulePath")
                addedCount++
                content += "\n$includeStatement"
            }
        }

        if (addedCount == 0) {
            logger.lifecycle("  All modules already included in settings")
        } else {
            logger.lifecycle("  Total modules added: $addedCount")
        }
    }

    private fun findModulesToInclude(submoduleDir: File, submoduleName: String): List<String> {
        val modules = mutableListOf<String>()

        if (submoduleDir.resolve("build.gradle.kts").exists()) {
            modules.add(":$submoduleName")
        }

        submoduleDir.listFiles()
            ?.filter { it.isDirectory }
            ?.forEach { moduleDir ->
                if (moduleDir.resolve("build.gradle.kts").exists()) {
                    modules.add(":$submoduleName:${moduleDir.name}")
                    logger.debug("  Found module: ${moduleDir.name}")
                }
            }

        return modules.sorted()
    }

    private fun findSettingsFile(): File? {
        return project.rootDir.resolve("settings.gradle.kts").takeIf { it.exists() }
            ?: project.rootDir.resolve("settings.gradle").takeIf { it.exists() }
    }

    private fun updateRootBuildGradle() {
        logger.lifecycle("Updating root build.gradle.kts...")

        val buildFile = project.rootDir.resolve("build.gradle.kts")
        if (!buildFile.exists()) {
            logger.warn("  Root build.gradle.kts not found")
            return
        }

        var content = buildFile.readText()

        if (!content.contains("plugins {")) {
            logger.warn("  No plugins block found in root build.gradle.kts")
            return
        }

        if (!content.contains("androidApplication")) {
            val pluginLine = "    alias(libs.plugins.androidApplication) apply false"
            content = content.replaceFirst("plugins {", "plugins {\n$pluginLine")
            buildFile.writeText(content)
            logger.lifecycle("  Added androidApplication plugin")
        } else {
            logger.info("  androidApplication plugin already present")
        }
    }
}