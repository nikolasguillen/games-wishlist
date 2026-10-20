package com.nikolasguillen.questlog.core.ui.util

import coil3.request.ImageRequest

/**
 * Asks for a picture whose pixels can be read back, which [sampleLogoBackdrop] needs. On Android that means no
 * hardware bitmap, since one cannot be drawn into the software copy it reads; elsewhere every bitmap is readable and
 * the request is returned as is. Only worth setting on a picture that is going to be sampled.
 */
expect fun ImageRequest.Builder.readableBitmaps(): ImageRequest.Builder
