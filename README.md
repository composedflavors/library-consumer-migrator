# Library Consumer Migrator

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.composedflavors.library-consumer-migrator?label=Gradle%20Plugin%20Portal)](https://plugins.gradle.org/plugin/io.composedflavors.library-consumer-migrator)
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

Add to your library project's `build.gradle.kts`:

```kotlin
plugins {
    id("io.composedflavors.library-consumer-migrator") version "0.0.1-alpha"
}
