package com.nikolasguillen.questlog.core.ui.component.translation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Desktop has no on-device language model, so there is no engine to credit.
@Composable
internal actual fun TranslationEngineBadge(modifier: Modifier) = Unit
