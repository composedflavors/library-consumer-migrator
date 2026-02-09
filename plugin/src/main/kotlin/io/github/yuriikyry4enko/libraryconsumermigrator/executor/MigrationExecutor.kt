package io.github.yuriikyry4enko.libraryconsumermigrator.executor

import io.github.yuriikyry4enko.libraryconsumermigrator.operations.DependencyMerger
import io.github.yuriikyry4enko.libraryconsumermigrator.operations.FileOperations
import io.github.yuriikyry4enko.libraryconsumermigrator.operations.GitOperations
import io.github.yuriikyry4enko.libraryconsumermigrator.operations.ProjectUpdater
import org.gradle.api.Project
import org.gradle.api.logging.Logger

/**
 * Orchestrates the migration process by coordinating different operations.
 */
class MigrationExecutor(
    private val project: Project,
    private val logger: Logger
) {
    private val gitOperations =
        GitOperations(project, logger)
    private val fileOperations =
        FileOperations(project, logger)
    private val dependencyMerger =
        DependencyMerger(project, logger)
    private val projectUpdater =
        ProjectUpdater(project, logger)

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