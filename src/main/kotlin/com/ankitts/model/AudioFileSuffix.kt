package com.ankitts.model

/**
 * Maps column types to their corresponding filename suffixes.
 * This enum standardizes the audio file naming convention across the application.
 */
enum class AudioFileSuffix(val suffix: String) {
    WORD(""),
    MEANING("-M"),
    EXAMPLE("-Ex"),
    PLURAL_FORM("-Pl"),
    PAST_SIMPLE("-Ps"),
    PAST_PARTICIPLE("-Pp");

    /**
     * Returns the complete filename by concatenating baseWord, the suffix, and the .wav extension.
     * Sanitizes the baseWord to avoid invalid filesystem characters.
     */
    fun getFullFileName(baseWord: String): String {
        val sanitizedBaseWord = sanitizeFileName(baseWord)
        return "$sanitizedBaseWord$suffix.wav"
    }

    /**
     * Sanitizes a filename by removing or replacing invalid filesystem characters.
     * - Trims whitespace
     * - Replaces whitespace with underscores
     * - Removes or substitutes characters like /:*?"<>|
     */
    private fun sanitizeFileName(fileName: String): String {
        return fileName
            .trim()
            .replace(Regex("\\s+"), "_") // Replace whitespace with underscores
            .replace(Regex("[/:*?\"<>|]"), "_") // Replace invalid filesystem characters with underscores
            .replace(Regex("_{2,}"), "_") // Replace multiple consecutive underscores with single underscore
            .trimEnd('_') // Remove trailing underscores
    }
}
