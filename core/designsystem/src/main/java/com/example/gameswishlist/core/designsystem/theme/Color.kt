package com.example.gameswishlist.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Spotify Palette
val SpotifyGreen = Color(0xFF1DB954)
val SpotifyBlack = Color(0xFF121212)
val SpotifyDarkGrey = Color(0xFF181818)
val SpotifyMediumGrey = Color(0xFF282828)
val SpotifyLightGrey = Color(0xFFB3B3B3)
val SpotifyWhite = Color(0xFFFFFFFF)

// Keep existing names if they are used elsewhere, but update values for Dark theme
val PrimaryDark = SpotifyGreen
val OnPrimaryDark = Color.Black
val PrimaryContainerDark = SpotifyGreen // Darker green
val OnPrimaryContainerDark = SpotifyBlack

val SecondaryDark = SpotifyLightGrey
val OnSecondaryDark = SpotifyBlack
val SecondaryContainerDark = Color(0xFF3E3E3E)
val OnSecondaryContainerDark = SpotifyWhite

val TertiaryDark = SpotifyWhite
val OnTertiaryDark = SpotifyBlack
val TertiaryContainerDark = Color(0xFF404040)
val OnTertiaryContainerDark = SpotifyWhite

val ErrorDark = Color(0xFFF2B8B5)
val OnErrorDark = Color(0xFF601410)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFF9DEDC)

val BackgroundDark = SpotifyBlack
val OnBackgroundDark = SpotifyWhite
val SurfaceDark = SpotifyDarkGrey
val OnSurfaceDark = SpotifyWhite

// Light colors (even if not used, keeping them for compilation if needed, or we can remove them if we enforce dark)
// For now, let's keep them as a fallback but we will force Dark in Theme.kt
val PrimaryLight = Color(0xFF1DB954)
val OnPrimaryLight = Color.White
val PrimaryContainerLight = Color(0xFFB1F4B1)
val OnPrimaryContainerLight = Color(0xFF002108)

val SecondaryLight = Color(0xFF53634F)
val OnSecondaryLight = Color.White
val SecondaryContainerLight = Color(0xFFD6E8CE)
val OnSecondaryContainerLight = Color(0xFF111F0F)

val TertiaryLight = Color(0xFF386567)
val OnTertiaryLight = Color.White
val TertiaryContainerLight = Color(0xFFBCEBEB)
val OnTertiaryContainerLight = Color(0xFF002021)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color.White
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val BackgroundLight = Color(0xFFFBFDF7)
val OnBackgroundLight = Color(0xFF1A1C19)
val SurfaceLight = Color(0xFFFBFDF7)
val OnSurfaceLight = Color(0xFF1A1C19)
