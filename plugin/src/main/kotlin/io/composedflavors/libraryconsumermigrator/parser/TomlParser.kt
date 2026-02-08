package io.composedflavors.libraryconsumermigrator.parser

import java.io.File

/**
 * Parses and merges TOML version catalog files.
 * This class is independent of Gradle and can be unit tested easily.
 */
class TomlParser {

    /**
     * Result of a catalog merge operation.
     *
     * @property hasChanges Whether any new entries were added
     * @property addedCount Total number of entries added
     * @property addedBySection Breakdown of added entries by section
     */
    data class MergeResult(
        val hasChanges: Boolean,
        val addedCount: Int,
        val addedBySection: Map<String, Int> = emptyMap()
    )

    /**
     * Merges version catalogs from source to target file.
     * Only adds entries that don't exist in the target.
     */
    fun mergeCatalogs(
        sourceFile: File,
        targetFile: File,
        sourceName: String
    ): MergeResult {
        val sourceContent = sourceFile.readText()
        val targetContent = targetFile.readText()

        val sourceSections = TOML_SECTIONS.associateWith {
            extractSectionMap(sourceContent, it)
        }

        val targetSections = TOML_SECTIONS.associateWith {
            extractSectionMap(targetContent, it)
        }

        val newEntries = TOML_SECTIONS.associateWith { section ->
            sourceSections[section]!!.filterKeys { key ->
                !targetSections[section]!!.containsKey(key)
            }
        }

        val addedBySection = newEntries.mapValues { it.value.size }
            .filterValues { it > 0 }

        val totalNew = newEntries.values.sumOf { it.size }

        if (totalNew > 0) {
            val updatedContent = mergeNewEntries(targetContent, newEntries, sourceName)
            targetFile.writeText(updatedContent)
        }

        return MergeResult(
            hasChanges = totalNew > 0,
            addedCount = totalNew,
            addedBySection = addedBySection
        )
    }

    /**
     * Extracts all key-value pairs from a specific TOML section.
     */
    private fun extractSectionMap(content: String, sectionName: String): Map<String, String> {
        val sectionPattern = Regex(
            """\[$sectionName\](.*?)(?=\n\[|\z)""",
            RegexOption.DOT_MATCHES_ALL
        )
        val match = sectionPattern.find(content) ?: return emptyMap()

        val entries = mutableMapOf<String, String>()
        val lines = match.groupValues[1].trim().lines()

        var currentKey: String? = null
        var currentValue = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()

            when {
                trimmed.isBlank() || trimmed.startsWith("#") -> continue

                '=' in trimmed && !line.startsWithWhitespace() -> {
                    // Save previous entry
                    currentKey?.let { entries[it] = currentValue.toString().trim() }

                    // Start new entry
                    val (key, value) = trimmed.split('=', limit = 2)
                    currentKey = key.trim()
                    currentValue = StringBuilder(value.trim())
                }

                currentKey != null -> {
                    // Multi-line value continuation
                    currentValue.append(" ").append(trimmed)
                }
            }
        }

        // Save last entry
        currentKey?.let { entries[it] = currentValue.toString().trim() }

        return entries
    }

    /**
     * Merges new entries into the target content.
     */
    private fun mergeNewEntries(
        content: String,
        newEntries: Map<String, Map<String, String>>,
        sourceName: String
    ): String {
        var result = content

        newEntries.forEach { (sectionName, entries) ->
            if (entries.isNotEmpty()) {
                val newContent = buildString {
                    appendLine()
                    appendLine("# Merged from $sourceName")
                    entries.forEach { (key, value) ->
                        appendLine("$key = $value")
                    }
                }
                result = appendToSection(result, sectionName, newContent)
            }
        }

        return result
    }

    /**
     * Appends content to a specific TOML section.
     */
    private fun appendToSection(
        content: String,
        sectionName: String,
        newContent: String
    ): String {
        val sectionPattern = Regex(
            """(\[$sectionName\])(.*?)(?=\n\[|\z)""",
            RegexOption.DOT_MATCHES_ALL
        )

        return sectionPattern.replace(content) { matchResult ->
            val header = matchResult.groupValues[1]
            val body = matchResult.groupValues[2]
            "$header$body$newContent\n"
        }
    }

    private fun String.startsWithWhitespace(): Boolean =
        firstOrNull()?.isWhitespace() == true

    companion object {
        private val TOML_SECTIONS = listOf("versions", "libraries", "plugins")
    }
}