package com.nikolasguillen.questlog.core.data.mapper

import com.nikolasguillen.questlog.core.ai.GeminiNanoDownload
import com.nikolasguillen.questlog.core.ai.GeminiNanoStatus
import com.nikolasguillen.questlog.core.model.TranslationModelDownload
import com.nikolasguillen.questlog.core.model.TranslationModelStatus

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
