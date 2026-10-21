package com.nikolasguillen.questlog.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Blue accent ramp. The primary is one blue per scheme, picked so it clears 4.5:1 as text and icons on that
// scheme's own surfaces: that is why accent text and icons use `primary` directly and there is no separate
// "readable on this surface" token. Everything else in the ramp is a tint of the same hue.
val BluePrimaryLight = Color(0xFF2C57D0) // The accent in the light scheme (6.2:1 on white, 4.9:1 on SoftGrey)
val BluePrimaryDark = Color(0xFFA8C7FA) // The accent in the dark scheme (10.3:1 on SurfaceDark)
val BlueOnPrimaryDark = Color(0xFF062E6F) // Content sitting on BluePrimaryDark
val BlueMuted = Color(0xFF6E7C99) // Dimmed: borders and low-emphasis accents
val BlueDeepOutline = Color(0xFF2F3748) // Dark-scheme dividers and subtle borders

// Neutral ramp. Stays achromatic so the blue is the only colour carrying meaning.
val NeutralBlack = Color(0xFF121212)
val NeutralDarkGrey = Color(0xFF181818)
val NeutralMediumGrey = Color(0xFF282828)
val NeutralLightGrey = Color(0xFFB3B3B3)
val NeutralWhite = Color(0xFFFFFFFF)

val ErrorRed = Color(0xFFCB1914)

// The two tiles a company logo sits on: a light logo goes on the dark one and a dark logo on the light one. Both are
// the same in either theme; the theme only picks which shows while the logo loads.
val LogoTileSurfaceLight = Color(0xFFF4F4F4)
val LogoTileSurfaceDark = NeutralMediumGrey

val PrimaryDark = BluePrimaryDark
val OnPrimaryDark = BlueOnPrimaryDark
val PrimaryContainerDark = BluePrimaryDark
val OnPrimaryContainerDark = BlueOnPrimaryDark

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
val ErrorContainerDark = ErrorRed
val OnErrorContainerDark = NeutralWhite

val BackgroundDark = NeutralBlack
val OnBackgroundDark = NeutralWhite
val SurfaceDark = NeutralDarkGrey
val OnSurfaceDark = NeutralWhite

// M3's baseline dark outlines are purple-tinted, which reads as a second hue next to the blue.
val OutlineDark = BlueMuted
val OutlineVariantDark = BlueDeepOutline

// Light neutral ramp. A 3-step elevation scale (background -> surface -> bar/container), mirroring the
// dark ramp's multiple steps. "On-light" text/icon colors reuse NeutralBlack/NeutralDarkGrey above rather
// than adding new dark tokens — they already clear WCAG AA against every step here.
val NeutralOffWhite = Color(0xFFFAFAFA) // Lightest: app background
val NeutralPaleGrey = Color(0xFFF0F0F0) // Surfaces and cards
val NeutralSoftGrey = Color(0xFFE4E4E4) // Bars, containers, dividers

// Fixed semantic indicators: same meaning regardless of appearance, so both color schemes share them.
val HypeRed = Color(0xFFF44336)
val RatingAmber = Color(0xFFFFB300)

// Score tiers (high / mid / low). Unlike the indicators above these are drawn as bare text and icons
// directly on a surface, so each scheme needs its own shade: the dark-scheme values are ~1.4-3.2:1 on the
// light surfaces, the light-scheme ones are darkened to ~4.7-4.9:1 on SurfaceLight.
val RatingHighDark = Color(0xFF4CAF50)
val RatingMidDark = Color(0xFFFFC107)
val RatingLowDark = Color(0xFFF44336)
val RatingHighLight = Color(0xFF1B7A2E)
val RatingMidLight = Color(0xFFA68024)
val RatingLowLight = Color(0xFFC62828)

// Light scheme. The light primary is a deeper blue than the dark one (the dark scheme's pale blue would
// vanish on a light surface), and it clears 4.5:1 on every light surface, so it doubles as accent text and icon
// colour. Containers reuse the primary fill with white content.
val PrimaryLight = BluePrimaryLight
val OnPrimaryLight = NeutralWhite
val PrimaryContainerLight = BluePrimaryLight
val OnPrimaryContainerLight = NeutralWhite

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
val ErrorContainerLight = ErrorRed
val OnErrorContainerLight = NeutralWhite

val BackgroundLight = NeutralOffWhite
val OnBackgroundLight = NeutralBlack
val SurfaceLight = NeutralPaleGrey
val OnSurfaceLight = NeutralBlack

val OutlineLight = BlueMuted
val OutlineVariantLight = NeutralLightGrey
