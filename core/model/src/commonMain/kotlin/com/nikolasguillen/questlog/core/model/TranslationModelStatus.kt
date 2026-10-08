package com.nikolasguillen.questlog.core.model

/**
 * State of the on-device translation model for the current user.
 *
 * [UNSUPPORTED] covers two cases callers cannot and need not tell apart: the hardware can never run the
 * model, and the device's language is already English so translation would be a no-op.
 */
enum class TranslationModelStatus { UNSUPPORTED, DOWNLOADABLE, DOWNLOADING, READY }
