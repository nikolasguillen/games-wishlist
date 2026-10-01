package com.nikolasguillen.questlog.core.designsystem.theme

import androidx.compose.ui.tooling.preview.PreviewLightDark

/**
 * Multipreview annotation that renders a composable in both light and dark theme.
 *
 * Use this on every `@Composable` preview in place of `@Preview` so previews stay in sync with
 * [QuestLogTheme] without each one hand-rolling a light/dark pair. Requires the previewed
 * composable to be wrapped in [QuestLogTheme] with no `darkTheme` argument, so it resolves from
 * `isSystemInDarkTheme()`, which Android Studio drives via this annotation's `uiMode`.
 */
@PreviewLightDark
annotation class QuestLogPreviews
