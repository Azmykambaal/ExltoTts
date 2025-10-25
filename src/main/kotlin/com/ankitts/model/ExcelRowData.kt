package com.ankitts.model

/**
 * Represents a single raw row from the Excel file with all 30 columns (A through AD).
 * This class serves as the raw data container before filtering and grouping.
 */
data class ExcelRowData(
    val rowNumber: Int,
    val colA_word: String?,
    val colB_partOfSpeech: String?,
    val colC_ipa: String?,
    val colD_meaning: String?,
    val colE_example: String?,
    val colF_extraNotes: String?,
    val colG_ankiDone: String?,
    val colH_word: String?,
    val colI_ipa: String?,
    val colJ_meaning: String?,
    val colK_example: String?,
    val colL_pluralForm: String?,
    val colM_plIpa: String?,
    val colN_plExample: String?,
    val colO_extraNotes: String?,
    val colP_pluralExtraNotes: String?,
    val colQ_ankiDone: String?,
    val colR_word: String?,
    val colS_ipa: String?,
    val colT_meaning: String?,
    val colU_example: String?,
    val colV_pastSimple: String?,
    val colW_psIpa: String?,
    val colX_psExample: String?,
    val colY_pastParticiple: String?,
    val colZ_ppIpa: String?,
    val colAA_ppExample: String?,
    val colAB_extraNotes: String?,
    val colAC_pastExtraNotes: String?,
    val colAD_ankiDone: String?
)
