package io.smokedsalmon.libraryconsumermigrator.task

import io.smokedsalmon.libraryconsumermigrator.LibraryConsumerMigratorExtension
import io.smokedsalmon.libraryconsumermigrator.executor.MigrationExecutor
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * Task that migrates a consumer application into the library project as a Git submodule.
 */
abstract class MigrateConsumerAppTask : DefaultTask() {

    @get:Internal
    internal val extension: LibraryConsumerMigratorExtension
        get() = project.extensions.getByType(LibraryConsumerMigratorExtension::class.java)


    @TaskAction
    fun migrate() {
        val extension = project.extensions.getByType(LibraryConsumerMigratorExtension::class.java)

        val path = extension.submodulePath.orNull
            ?: throw GradleException(
                """
                |Property 'submodulePath' is required.
                |
                |Configure it in build.gradle.kts:
                |  libraryConsumerMigrator {
                |      submodulePath.set("https://github.com/user/repo.git")
                |  }
                |
                |Or via command line:
                |  ./gradlew migrateConsumerApp -PsubmodulePath=<git-url>
                """.trimMargin()
            )

        val name = extension.submoduleName.get()
        val branch = extension.branch.orNull

        logger.lifecycle("Importing example application as test submodule...")

        val executor = MigrationExecutor(project, logger)

        val result = executor.execute(
            submodulePath = path,
            submoduleName = name,
            branch = branch
        )

        result.fold(
            onSuccess = { printSuccessMessage(name) },
            onFailure = { error ->
                throw GradleException(
                    "Migration failed: ${error.message}\n" +
                            "Submodule path: $path\n" +
                            "Submodule name: $name",
                    error
                )
            }
        )
    }

    private fun printSuccessMessage(submoduleName: String) {
        logger.lifecycle("")
        logger.lifecycle("✓ Example application imported successfully!")
        logger.lifecycle("")
        logger.lifecycle("Next steps:")
        logger.lifecycle("  1. Review merged dependencies in gradle/libs.versions.toml")
        logger.lifecycle("  2. Add implementation(project(\":your-library\")) to $submoduleName/build.gradle.kts")
        logger.lifecycle("  3. Sync project with Gradle files")
        logger.lifecycle("  4. Run the example application to test your library")
    }
}