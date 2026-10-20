package com.nikolasguillen.questlog.core.ui.util

import coil3.request.ImageRequest
import coil3.request.allowHardware

actual fun ImageRequest.Builder.readableBitmaps(): ImageRequest.Builder = allowHardware(false)
