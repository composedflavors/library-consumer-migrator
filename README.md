# Library Consumer Migrator

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.yuriikyry4enko.library-consumer-migrator?label=Gradle%20Plugin%20Portal)](https://plugins.gradle.org/plugin/io.github.yuriikyry4enko.library-consumer-migrator)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

A Gradle plugin that automates the integration of consumer applications as Git submodules into library projects, enabling streamlined testing and development in a unified workspace.

## 🎯 Why Use This Plugin?

When developing a library (especially Kotlin Multiplatform), you often need to test it with a real consumer application. This plugin automates:

- ✅ Adding consumer app as Git submodule
- ✅ Cleaning up standalone project files
- ✅ Merging dependencies to root version catalog
- ✅ Configuring project settings automatically
- ✅ Multi-module detection and setup

## 📦 Installation

### Using Plugins DSL (Recommended)

Add to your library project's root `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.yuriikyry4enko.library-consumer-migrator") version "0.0.1-alpha"
}
```

## 🚀 Quick Start

### Basic Usage (Command Line)

Run the migration task with required parameters:

```bash
./gradlew migrateConsumerApp \
  -PsubmodulePath="https://github.com/username/consumer-app.git" \
  -PsubmoduleName="example-app" \
  -Pbranch="develop" \
  -PmodulesToInclude="composeApp,shared"

```

Configuration via Extension
Configure the plugin in your `build.gradle.kts`:

```kotlin
libraryConsumerMigrator {
    submodulePath.set("https://github.com/username/consumer-app.git")
    submoduleName.set("example-app")
    branch.set("main") // Optional: defaults to repository's default branch
    modulesToInclude.set(listOf("composeApp", "shared")) // Optional: include specific modules
}
```


Then run: 

```bash
./gradlew migrateConsumerApp
```

## 📝 Example Workflow

Scenario: Testing Your KMP Library
- You're developing a Kotlin Multiplatform library and want to test it with a real Android/iOS app:

- Apply the plugin to your library's build.gradle.kts

- Configure pointing to your test app repository

- Run migration: `./gradlew migrateConsumerApp`

- Develop: Make changes to your library and immediately test in the integrated app

- Commit: Git tracks both your library changes and submodule reference


## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](https://www.apache.org/licenses/LICENSE-2.0.txt) file for details.
