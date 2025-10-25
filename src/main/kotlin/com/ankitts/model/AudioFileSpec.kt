package com.ankitts.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Encapsulates all information needed for TTS conversion of a single text field.
 * This class serves as a specification object passed to the TTS service, 
 * decoupling domain logic from TTS implementation details.
 */
@Parcelize
data class AudioFileSpec(
    val textToConvert: String,
    val fileName: String,
    val language: String = "en-US",
    val speechRate: Float = 0.6f,
    val outputDirectory: String? = null
) : Parcelable
