package com.ankitts.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the third group extracted from columns R, T, U, V, and Y.
 * This class represents verbs with irregular past tense forms.
 */
@Parcelize
data class IrregularVerb(
    val word: String,
    val meaning: String?,
    val example: String?,
    val pastSimple: String?,
    val pastParticiple: String?
) : WordEntry, Parcelable {
    /**
     * Returns the word value for audio file naming.
     */
    fun getBaseFileName(): String = word
}
