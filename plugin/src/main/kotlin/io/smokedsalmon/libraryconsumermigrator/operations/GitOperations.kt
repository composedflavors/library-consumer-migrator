package io.smokedsalmon.libraryconsumermigrator.operations

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File
import java.io.ByteArrayOutputStream

/**
 * Handles all Git-related operations for submodule management.
 */
class GitOperations(
    private val project: Project,
    private val logger: Logger
) {

    fun addSubmodule(submodulePath: String, submoduleName: String, branch: String?) {
        logger.lifecycle("Adding git submodule from $submodulePath...")
        logger.lifecycle("  Target directory: $submoduleName")
        branch?.let { logger.lifecycle("  Branch: $it") }

        val submoduleDir = project.rootDir.resolve(submoduleName)

        when {
            submoduleDir.existsWithContent() -> {
                updateExistingSubmodule(submoduleDir, submoduleName, branch)
            }
            else -> {
                createNewSubmodule(submodulePath, submoduleName, branch, submoduleDir)
            }
        }
    }

    private fun updateExistingSubmodule(
        submoduleDir: File,
        submoduleName: String,
        branch: String?
    ) {
        logger.lifecycle("  Directory exists, updating...")

        branch?.let {
            logger.info("  Checking out branch: $it")
            val checkoutResult = executeGit(
                listOf("checkout", it),
                workingDir = submoduleDir
            )
            if (checkoutResult.exitCode != 0) {
                logger.warn("  Checkout failed: ${checkoutResult.error}")
            }
        }

        val updateResult = executeGit(
            listOf("submodule", "update", "--init", "--recursive", "--remote", submoduleName)
        )

        if (updateResult.exitCode == 0) {
            logger.lifecycle("  Submodule updated successfully")
        } else {
            logger.error("  Update failed: ${updateResult.error}")
            throw RuntimeException("Failed to update submodule: ${updateResult.error}")
        }
    }

    private fun createNewSubmodule(
        submodulePath: String,
        submoduleName: String,
        branch: String?,
        submoduleDir: File
    ) {
        logger.info("  Creating new submodule...")

        val addCommand = buildSubmoduleAddCommand(submodulePath, submoduleName, branch)
        logger.lifecycle("  Executing: git ${addCommand.joinToString(" ")}")

        val addResult = executeGit(addCommand)

        if (addResult.exitCode != 0) {
            logger.warn("  Git submodule add failed: ${addResult.error}")
            logger.warn("  Trying alternative approach...")
            cloneDirectly(submodulePath, submoduleName, branch, submoduleDir)
            return
        }

        val initResult = executeGit(listOf("submodule", "init", submoduleName))
        if (initResult.exitCode != 0) {
            logger.warn("  Submodule init warning: ${initResult.error}")
        }

        val updateResult = executeGit(
            listOf("submodule", "update", "--init", "--recursive", submoduleName)
        )

        if (updateResult.exitCode != 0) {
            logger.warn("  Submodule update warning: ${updateResult.error}")
        }

        if (!submoduleDir.existsWithContent()) {
            logger.warn("  Submodule directory empty after add, trying direct clone fallback...")
            cloneDirectly(submodulePath, submoduleName, branch, submoduleDir)
        } else {
            val fileCount = submoduleDir.listFiles()?.size ?: 0
            logger.lifecycle("  Submodule downloaded ($fileCount items)")
        }
    }

    private fun cloneDirectly(
        submodulePath: String,
        submoduleName: String,
        branch: String?,
        submoduleDir: File
    ) {
        logger.lifecycle("  Using direct clone approach...")

        if (submoduleDir.exists()) {
            submoduleDir.deleteRecursively()
            logger.debug("  Cleaned up empty directory")
        }

        val cloneCommand = buildCloneCommand(submodulePath, submoduleName, branch)
        logger.lifecycle("  Executing: git ${cloneCommand.joinToString(" ")}")

        val cloneResult = executeGit(cloneCommand)

        if (cloneResult.exitCode == 0) {
            logger.lifecycle("  Direct clone successful")

            if (submoduleDir.existsWithContent()) {
                val fileCount = submoduleDir.listFiles()?.size ?: 0
                logger.lifecycle("  Downloaded $fileCount items")

                addToGitmodules(submodulePath, submoduleName, branch)

                registerAsSubmodule(submoduleDir, submodulePath)
            } else {
                throw RuntimeException("Clone succeeded but directory is empty")
            }
        } else {
            throw RuntimeException(
                "Failed to clone submodule from $submodulePath\n" +
                        "Error: ${cloneResult.error}\n" +
                        "Output: ${cloneResult.output}"
            )
        }
    }

    private fun registerAsSubmodule(submoduleDir: File, submodulePath: String) {
        logger.debug("  Registering as submodule in git...")

        val addResult = executeGit(listOf("add", submoduleDir.name))
        if (addResult.exitCode != 0) {
            logger.warn("  Could not register submodule in git index: ${addResult.error}")
        }
    }

    private fun addToGitmodules(submodulePath: String, submoduleName: String, branch: String?) {
        val gitmodulesFile = project.rootDir.resolve(".gitmodules")

        if (gitmodulesFile.exists()) {
            val existingContent = gitmodulesFile.readText()
            if (existingContent.contains("[submodule \"$submoduleName\"]")) {
                logger.debug("  Submodule already in .gitmodules")
                return
            }
        }

        val content = buildString {
            if (gitmodulesFile.exists() && gitmodulesFile.readText().isNotBlank()) {
                appendLine()
            }
            appendLine("[submodule \"$submoduleName\"]")
            appendLine("\tpath = $submoduleName")
            appendLine("\turl = $submodulePath")
            branch?.let { appendLine("\tbranch = $it") }
        }
        gitmodulesFile.appendText(content)
        logger.debug("  Added submodule to .gitmodules")
    }

    private fun buildSubmoduleAddCommand(
        path: String,
        name: String,
        branch: String?
    ): List<String> {
        return buildList {
            add("submodule")
            add("add")
            add("--force")
            branch?.let {
                add("-b")
                add(it)
            }
            add(path)
            add(name)
        }
    }

    private fun buildCloneCommand(
        path: String,
        name: String,
        branch: String?
    ): List<String> {
        return buildList {
            add("clone")
            branch?.let {
                add("-b")
                add(it)
            }
            add("--depth")
            add("1")
            add(path)
            add(name)
        }
    }

    private fun executeGit(
        args: List<String>,
        workingDir: File = project.rootDir
    ): GitResult {
        val fullCommand = listOf("git") + args
        logger.debug("  Working directory: ${workingDir.absolutePath}")
        logger.debug("  Executing: ${fullCommand.joinToString(" ")}")

        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()

        val exitCode = project.exec { spec ->
            spec.workingDir = workingDir
            spec.commandLine = fullCommand
            spec.isIgnoreExitValue = true
            spec.standardOutput = outputStream
            spec.errorOutput = errorStream
        }.exitValue

        val output = outputStream.toString().trim()
        val error = errorStream.toString().trim()

        if (output.isNotBlank()) {
            logger.debug("  Output: $output")
        }
        if (error.isNotBlank() && exitCode != 0) {
            logger.debug("  Error: $error")
        }

        return GitResult(exitCode, output, error)
    }

    private fun File.existsWithContent(): Boolean =
        exists() && isDirectory && listFiles()?.isNotEmpty() == true

    private data class GitResult(
        val exitCode: Int,
        val output: String,
        val error: String
    )
}