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
val PrimaryContainerDark = Color(0xFF0C4D21) // A deeper, darker green for container
val OnPrimaryContainerDark = Color(0xFFB3F2C7) // A very light green for text on container

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