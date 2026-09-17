package com.example.gameswishlist.core.ai

sealed interface GeminiNanoDownload {
    data class Progress(val fraction: Float?) : GeminiNanoDownload
    data object Completed : GeminiNanoDownload
    data object Failed : GeminiNanoDownload
}
