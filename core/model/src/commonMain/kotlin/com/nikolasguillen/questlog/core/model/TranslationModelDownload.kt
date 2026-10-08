package com.nikolasguillen.questlog.core.model

sealed interface TranslationModelDownload {
    data class InProgress(val fraction: Float?) : TranslationModelDownload
    data object Completed : TranslationModelDownload
    data object Failed : TranslationModelDownload
}
