package com.nikolasguillen.questlog.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Gold accent ramp. One hue (~44°) at four intensities: every accent in the app is one of these,
// so a highlight and the surface behind it always read as the same colour at different strengths.
val GoldDeep = Color(0xFFA68024) // Darkest: container fills behind gold content
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

// Light neutral ramp. A 3-step elevation scale (background -> surface -> bar/container), mirroring the
// dark ramp's multiple steps. "On-light" text/icon colors reuse NeutralBlack/NeutralDarkGrey above rather
// than adding new dark tokens — they already clear WCAG AA against every step here.
val NeutralOffWhite = Color(0xFFFAFAFA) // Lightest: app background
val NeutralPaleGrey = Color(0xFFF0F0F0) // Surfaces and cards
val NeutralSoftGrey = Color(0xFFE4E4E4) // Bars, containers, dividers

// Fixed semantic indicators: same meaning regardless of appearance, so both color schemes share them.
val HypeRed = Color(0xFFF44336)
val RatingAmber = Color(0xFFFFB300)

// Light scheme. Gold keeps the same hue as the dark scheme throughout (FR-004): where it doesn't clear
// 4.5:1 as small text on a light surface (it doesn't — ~1.7:1 as body text on white), it is used as a
// container/accent fill with a dark "on" color instead, the same shape OnPrimaryDark = Color.Black already
// uses. PrimaryContainerLight/OnPrimaryContainerLight simply swap GoldDeep/GoldBright's roles from the dark
// scheme (8.9:1) rather than inventing a third gold shade.
val PrimaryLight = Gold
val OnPrimaryLight = NeutralBlack
val PrimaryContainerLight = GoldBright
val OnPrimaryContainerLight = GoldDeep

val SecondaryLight = NeutralMediumGrey
val OnSecondaryLight = NeutralWhite
val SecondaryContainerLight = NeutralPaleGrey
val OnSecondaryContainerLight = NeutralDarkGrey

val TertiaryLight = NeutralBlack
val OnTertiaryLight = NeutralWhite
val TertiaryContainerLight = NeutralSoftGrey
val OnTertiaryContainerLight = NeutralBlack

// M3's baseline light error tokens (verified to meet WCAG AA as part of the M3 spec itself), matching how
// ErrorDark/OnErrorDark/etc. above are M3's baseline dark error tokens.
val ErrorLight = Color(0xFFB3261E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)

val BackgroundLight = NeutralOffWhite
val OnBackgroundLight = NeutralBlack
val SurfaceLight = NeutralPaleGrey
val OnSurfaceLight = NeutralBlack

val OutlineLight = GoldMuted
val OutlineVariantLight = NeutralLightGrey
