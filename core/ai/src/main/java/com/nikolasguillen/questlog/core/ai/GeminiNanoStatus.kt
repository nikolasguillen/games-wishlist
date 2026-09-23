package com.nikolasguillen.questlog.core.ai

/** Mirrors `com.google.mlkit.genai.common.FeatureStatus`, so callers never see the raw ML Kit ints. */
enum class GeminiNanoStatus { UNAVAILABLE, DOWNLOADABLE, DOWNLOADING, AVAILABLE }
