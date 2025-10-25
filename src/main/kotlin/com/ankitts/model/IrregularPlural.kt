package com.ankitts.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the second group extracted from columns H, J, K, and L.
 * This class represents nouns with irregular plural forms that need special handling.
 */
@Parcelize
data class IrregularPlural(
    val word: String,
    val meaning: String?,
    val example: String?,
    val pluralForm: String?
) : WordEntry, Parcelable {
    /**
     * Returns the word value for audio file naming.
     */
    fun getBaseFileName(): String = word
}
