package com.ankitts.processor

import com.ankitts.model.*

/**
 * Main data processor that transforms raw ExcelRowData into domain models
 * and generates audio file specifications for TTS conversion.
 */
class DataProcessor {

    /**
     * Main processing method that orchestrates the entire transformation pipeline.
     * Transforms raw Excel data into three word groups and generates audio specs.
     */
    fun processData(excelRows: List<ExcelRowData>): ProcessedData {
        // Extract the three word groups
        val regularWords = extractRegularWords(excelRows)
        val irregularPlurals = extractIrregularPlurals(excelRows)
        val irregularVerbs = extractIrregularVerbs(excelRows)
        
        // Generate audio specifications directly from the lists
        val audioSpecs = generateAudioSpecs(regularWords, irregularPlurals, irregularVerbs)
        
        // Return final ProcessedData with all properties populated
        return ProcessedData(regularWords, irregularPlurals, irregularVerbs, audioSpecs)
    }

    /**
     * Extracts regular words from Excel data.
     * Filters rows where colA_word is non-null and non-blank.
     */
    private fun extractRegularWords(excelRows: List<ExcelRowData>): List<RegularWord> {
        return excelRows
            .filter { isValidWord(it.colA_word) }
            .map { row ->
                RegularWord(
                    word = trimAndNullify(row.colA_word)!!,
                    meaning = trimAndNullify(row.colD_meaning),
                    example = trimAndNullify(row.colE_example)
                )
            }
    }

    /**
     * Extracts irregular plurals from Excel data.
     * Filters rows where colH_word is non-null and non-blank.
     */
    private fun extractIrregularPlurals(excelRows: List<ExcelRowData>): List<IrregularPlural> {
        return excelRows
            .filter { isValidWord(it.colH_word) }
            .map { row ->
                IrregularPlural(
                    word = trimAndNullify(row.colH_word)!!,
                    meaning = trimAndNullify(row.colJ_meaning),
                    example = trimAndNullify(row.colK_example),
                    pluralForm = trimAndNullify(row.colL_pluralForm)
                )
            }
    }

    /**
     * Extracts irregular verbs from Excel data.
     * Filters rows where colR_word is non-null and non-blank.
     */
    private fun extractIrregularVerbs(excelRows: List<ExcelRowData>): List<IrregularVerb> {
        return excelRows
            .filter { isValidWord(it.colR_word) }
            .map { row ->
                IrregularVerb(
                    word = trimAndNullify(row.colR_word)!!,
                    meaning = trimAndNullify(row.colT_meaning),
                    example = trimAndNullify(row.colU_example),
                    pastSimple = trimAndNullify(row.colV_pastSimple),
                    pastParticiple = trimAndNullify(row.colY_pastParticiple)
                )
            }
    }

    /**
     * Generates audio file specifications for all non-null fields across all word groups.
     * Creates AudioFileSpec objects with proper naming conventions and TTS parameters.
     * Tracks emitted filenames to prevent collisions by appending stable disambiguators.
     */
    private fun generateAudioSpecs(
        regularWords: List<RegularWord>,
        irregularPlurals: List<IrregularPlural>,
        irregularVerbs: List<IrregularVerb>
    ): List<AudioFileSpec> {
        val audioSpecs = mutableListOf<AudioFileSpec>()
        val emittedFilenames = mutableSetOf<String>()
        
        // Generate specs for regular words
        regularWords.forEach { word ->
            audioSpecs.add(createAudioSpecWithCollisionDetection(word.getBaseFileName(), word.getBaseFileName(), AudioFileSuffix.WORD, "R", emittedFilenames))
            word.meaning?.let { 
                audioSpecs.add(createAudioSpecWithCollisionDetection(word.getBaseFileName(), it, AudioFileSuffix.MEANING, "R", emittedFilenames))
            }
            word.example?.let { 
                audioSpecs.add(createAudioSpecWithCollisionDetection(word.getBaseFileName(), it, AudioFileSuffix.EXAMPLE, "R", emittedFilenames))
            }
        }
        
        // Generate specs for irregular plurals
        irregularPlurals.forEach { plural ->
            audioSpecs.add(createAudioSpecWithCollisionDetection(plural.getBaseFileName(), plural.getBaseFileName(), AudioFileSuffix.WORD, "IP", emittedFilenames))
            plural.meaning?.let { 
                audioSpecs.add(createAudioSpecWithCollisionDetection(plural.getBaseFileName(), it, AudioFileSuffix.MEANING, "IP", emittedFilenames))
            }
            plural.example?.let { 
                audioSpecs.add(createAudioSpecWithCollisionDetection(plural.getBaseFileName(), it, AudioFileSuffix.EXAMPLE, "IP", emittedFilenames))
            }
            plural.pluralForm?.let { 
                audioSpecs.add(createAudioSpecWithCollisionDetection(plural.getBaseFileName(), it, AudioFileSuffix.PLURAL_FORM, "IP", emittedFilenames))
            }
        }
        
        // Generate specs for irregular verbs
        irregularVerbs.forEach { verb ->
            audioSpecs.add(createAudioSpecWithCollisionDetection(verb.getBaseFileName(), verb.getBaseFileName(), AudioFileSuffix.WORD, "IV", emittedFilenames))
            verb.meaning?.let { 
                audioSpecs.add(createAudioSpecWithCollisionDetection(verb.getBaseFileName(), it, AudioFileSuffix.MEANING, "IV", emittedFilenames))
            }
            verb.example?.let { 
                audioSpecs.add(createAudioSpecWithCollisionDetection(verb.getBaseFileName(), it, AudioFileSuffix.EXAMPLE, "IV", emittedFilenames))
            }
            verb.pastSimple?.let { 
                audioSpecs.add(createAudioSpecWithCollisionDetection(verb.getBaseFileName(), it, AudioFileSuffix.PAST_SIMPLE, "IV", emittedFilenames))
            }
            verb.pastParticiple?.let { 
                audioSpecs.add(createAudioSpecWithCollisionDetection(verb.getBaseFileName(), it, AudioFileSuffix.PAST_PARTICIPLE, "IV", emittedFilenames))
            }
        }
        
        return audioSpecs
    }

    /**
     * Creates an AudioFileSpec with collision detection and disambiguation.
     * Tracks emitted filenames to prevent duplicates by appending group-specific disambiguators.
     */
    private fun createAudioSpecWithCollisionDetection(
        baseFileName: String, 
        text: String, 
        suffix: AudioFileSuffix, 
        groupDisambiguator: String,
        emittedFilenames: MutableSet<String>
    ): AudioFileSpec {
        val originalFileName = suffix.getFullFileName(baseFileName)
        
        // Check if filename already exists
        if (emittedFilenames.contains(originalFileName)) {
            // Append group disambiguator to prevent collision
            val disambiguatedBaseFileName = "$baseFileName-$groupDisambiguator"
            val disambiguatedFileName = suffix.getFullFileName(disambiguatedBaseFileName)
            
            // Track the disambiguated filename
            emittedFilenames.add(disambiguatedFileName)
            
            return AudioFileSpec(
                textToConvert = text,
                fileName = disambiguatedFileName,
                language = "en-US",
                speechRate = 0.6f
            )
        } else {
            // Track the original filename
            emittedFilenames.add(originalFileName)
            
            return AudioFileSpec(
                textToConvert = text,
                fileName = originalFileName,
                language = "en-US",
                speechRate = 0.6f
            )
        }
    }

    /**
     * Creates an AudioFileSpec with standardized parameters.
     * @deprecated Use createAudioSpecWithCollisionDetection for new implementations
     */
    @Deprecated("Use createAudioSpecWithCollisionDetection to prevent filename collisions")
    private fun createAudioSpec(baseFileName: String, text: String, suffix: AudioFileSuffix): AudioFileSpec {
        return AudioFileSpec(
            textToConvert = text,
            fileName = suffix.getFullFileName(baseFileName),
            language = "en-US",
            speechRate = 0.6f
        )
    }

    /**
     * Trims whitespace from a string and returns null if the result is empty or if input was null.
     * Ensures consistent handling of empty/whitespace-only cells.
     */
    private fun trimAndNullify(value: String?): String? {
        return value?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * Returns true if the word is non-null and non-blank after trimming.
     * Used as a filter condition for all three groups.
     */
    private fun isValidWord(word: String?): Boolean {
        return word?.trim()?.isNotEmpty() == true
    }
}
