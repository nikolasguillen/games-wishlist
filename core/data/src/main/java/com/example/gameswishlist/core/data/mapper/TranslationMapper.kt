package com.example.gameswishlist.core.data.mapper

import com.example.gameswishlist.core.ai.GeminiNanoDownload
import com.example.gameswishlist.core.ai.GeminiNanoStatus
import com.example.gameswishlist.core.model.TranslationModelDownload
import com.example.gameswishlist.core.model.TranslationModelStatus

fun GeminiNanoStatus.toTranslationModelStatus(): TranslationModelStatus = when (this) {
    GeminiNanoStatus.AVAILABLE -> TranslationModelStatus.READY
    GeminiNanoStatus.DOWNLOADING -> TranslationModelStatus.DOWNLOADING
    GeminiNanoStatus.DOWNLOADABLE -> TranslationModelStatus.DOWNLOADABLE
    GeminiNanoStatus.UNAVAILABLE -> TranslationModelStatus.UNSUPPORTED
}

fun GeminiNanoDownload.toTranslationModelDownload(): TranslationModelDownload = when (this) {
    is GeminiNanoDownload.Progress -> TranslationModelDownload.InProgress(fraction)
    GeminiNanoDownload.Completed -> TranslationModelDownload.Completed
    GeminiNanoDownload.Failed -> TranslationModelDownload.Failed
}
