package com.ankitts.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the first group of words extracted from columns A, D, and E.
 * This class represents simple vocabulary words without irregular forms.
 */
@Parcelize
data class RegularWord(
    val word: String,
    val meaning: String?,
    val example: String?
) : WordEntry, Parcelable {
    /**
     * Returns the word value to be used as the base for audio file naming.
     */
    fun getBaseFileName(): String = word
}
