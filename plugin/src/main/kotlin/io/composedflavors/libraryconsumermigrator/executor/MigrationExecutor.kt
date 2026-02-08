package io.composedflavors.libraryconsumermigrator.executor

import io.composedflavors.libraryconsumermigrator.operations.DependencyMerger
import io.composedflavors.libraryconsumermigrator.operations.FileOperations
import io.composedflavors.libraryconsumermigrator.operations.GitOperations
import io.composedflavors.libraryconsumermigrator.operations.ProjectUpdater
import org.gradle.api.Project
import org.gradle.api.logging.Logger

/**
 * Orchestrates the migration process by coordinating different operations.
 */
class MigrationExecutor(
    private val project: Project,
    private val logger: Logger
) {
    private val gitOperations = GitOperations(project, logger)
    private val fileOperations = FileOperations(project, logger)
    private val dependencyMerger =
        DependencyMerger(project, logger)
    private val projectUpdater = ProjectUpdater(project, logger)

    /**
     * Executes the full migration process.
     *
     * @param submodulePath Git repository URL or path
     * @param submoduleName Name of the submodule directory
     * @param branch Optional Git branch to checkout
     * @return Result indicating success or failure
     */
    fun execute(
        submodulePath: String,
        submoduleName: String,
        branch: String?
    ): Result<Unit> = runCatching {
        gitOperations.addSubmodule(submodulePath, submoduleName, branch)
        fileOperations.removeStandaloneFiles(submoduleName)
        dependencyMerger.mergeDependencies(submoduleName)
        projectUpdater.updateProjectFiles(submoduleName)
    }
}