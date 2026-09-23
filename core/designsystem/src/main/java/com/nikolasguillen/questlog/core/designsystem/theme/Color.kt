package com.nikolasguillen.questlog.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Gold accent ramp. One hue (~44°) at four intensities: every accent in the app is one of these,
// so a highlight and the surface behind it always read as the same colour at different strengths.
val GoldDeep = Color(0xFF4A3A14) // Darkest: container fills behind gold content
val GoldMuted = Color(0xFF7C6E46) // Dimmed: borders and low-emphasis accents
val Gold = Color(0xFFE0BE62) // The accent itself: FAB, indicators, accent text and icons
val GoldBright = Color(0xFFF5E6BC) // Brightest: content sitting on a GoldDeep container

// Neutral ramp. Stays achromatic so the gold is the only colour carrying meaning.
val NeutralBlack = Color(0xFF121212)
val NeutralDarkGrey = Color(0xFF181818)
val NeutralMediumGrey = Color(0xFF282828)
val NeutralLightGrey = Color(0xFFB3B3B3)
val NeutralWhite = Color(0xFFFFFFFF)

val PrimaryDark = Gold
val OnPrimaryDark = Color.Black
val PrimaryContainerDark = GoldDeep
val OnPrimaryContainerDark = GoldBright

val SecondaryDark = NeutralLightGrey
val OnSecondaryDark = NeutralBlack
val SecondaryContainerDark = Color(0xFF3E3E3E)
val OnSecondaryContainerDark = NeutralWhite

val TertiaryDark = NeutralWhite
val OnTertiaryDark = NeutralBlack
val TertiaryContainerDark = Color(0xFF404040)
val OnTertiaryContainerDark = NeutralWhite

val ErrorDark = Color(0xFFF2B8B5)
val OnErrorDark = Color(0xFF601410)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFF9DEDC)

val BackgroundDark = NeutralBlack
val OnBackgroundDark = NeutralWhite
val SurfaceDark = NeutralDarkGrey
val OnSurfaceDark = NeutralWhite

// M3's baseline dark outlines are purple-tinted, which reads as a second hue next to the gold.
val OutlineDark = GoldMuted
val OutlineVariantDark = Color(0xFF433D2D)
