package com.ankitts.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Container for processed data results containing three word groups and audio specifications.
 * This class serves as the return type for DataProcessor.processData() and is consumed
 * by the preview UI and TTS modules.
 */
@Parcelize
data class ProcessedData(
    val regularWords: List<RegularWord>,
    val irregularPlurals: List<IrregularPlural>,
    val irregularVerbs: List<IrregularVerb>,
    val audioSpecs: List<AudioFileSpec>
) : Parcelable
