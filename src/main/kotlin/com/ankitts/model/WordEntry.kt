package com.ankitts.model

/**
 * Common interface for word groups to unify getBaseFileName functionality.
 * All word entry types should implement this interface to provide consistent
 * base file name generation for audio files.
 */
interface WordEntry {
    /**
     * Returns the base file name to be used for audio file naming.
     * @return The base file name string
     */
    fun getBaseFileName(): String
}
