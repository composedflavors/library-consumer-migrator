package io.github.yuriikyry4enko.libraryconsumermigrator

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Extension for configuring the Library Consumer Migrator plugin.
 *
 * Example usage:
 * ```kotlin
 * libraryConsumerMigrator {
 *     submodulePath.set("https://github.com/user/consumer-app.git")
 *     submoduleName.set("my-example-app")
 *     branch.set("develop")
 *     modulesToInclude.set(listOf("composeApp", "shared"))
 * }
 * ```
 */
abstract class LibraryConsumerMigratorExtension {

    /**
     * Git repository path or URL of the consumer application to import.
     * This property is required - task will fail if not set via extension or command line.
     */
    abstract val submodulePath: Property<String>

    /**
     * Name of the submodule directory to create.
     * Default value: "example-application"
     */
    abstract val submoduleName: Property<String>

    /**
     * Git branch to checkout in the submodule.
     * Optional. Uses repository's default branch if not specified.
     */
    abstract val branch: Property<String>

    /**
     * Modules to include from the submodule.
     * If empty, all modules with build.gradle.kts will be included.
     * Example: listOf("composeApp", "shared")
     */
    abstract val modulesToInclude: ListProperty<String>

    init {
        submoduleName.convention("example-application")
    }
}