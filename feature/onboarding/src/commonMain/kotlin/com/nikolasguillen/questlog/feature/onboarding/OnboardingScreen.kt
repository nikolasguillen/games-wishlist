@file:OptIn(ExperimentalComposeUiApi::class)

package com.nikolasguillen.questlog.feature.onboarding

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.LoadingPage
import com.nikolasguillen.questlog.core.ui.util.rememberNotificationPermissionState
import com.nikolasguillen.questlog.feature.onboarding.components.OnboardingBottomBar
import com.nikolasguillen.questlog.feature.onboarding.components.OnboardingInfoPage
import com.nikolasguillen.questlog.feature.onboarding.components.OnboardingPlatformsPage
import com.nikolasguillen.questlog.feature.onboarding.components.OnboardingRemindersPage
import com.nikolasguillen.questlog.feature.onboarding.mapper.toInfoUiModel
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingContentState
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingPage
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingUiEffect
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingUiEvent
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingUiState
import com.nikolasguillen.questlog.feature.onboarding.resources.Res
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_skip
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// viewModel is the same instance for the route's whole lifetime, so ref-comparison skips correctly.
@Suppress("ParamsComparedByRef")
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val permissionState = rememberNotificationPermissionState(
        onResult = { granted ->
            viewModel.onEvent(OnboardingUiEvent.NotificationPermissionResult(granted))
        }
    )
    val latestPermissionState = rememberUpdatedState(permissionState)
    // The effect below never restarts once launched, so it must read the latest callback rather than the
    // one captured on first composition.
    val latestOnFinish by rememberUpdatedState(onFinish)

    // Only a composable can read these, so the screen reports them once and the ViewModel decides which
    // pages follow from them.
    LaunchedEffect(viewModel) {
        viewModel.onEvent(
            OnboardingUiEvent.NotificationFactsResolved(
                requiresRuntimePermission = permissionState.requiresRuntimePermission,
                canDeliver = permissionState.canDeliver
            )
        )
    }

    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    OnboardingUiEffect.Finished -> latestOnFinish()

                    OnboardingUiEffect.RequestNotificationPermission -> {
                        val permission = latestPermissionState.value
                        if (permission.isPermanentlyDenied) {
                            // The system will not show its dialog again, and sending the user to system
                            // settings is not this flow's job: it explains instead, so this is a decline.
                            viewModel.onEvent(OnboardingUiEvent.NotificationPermissionResult(granted = false))
                        } else {
                            permission.request()
                        }
                    }
                }
            }
        }
    }

    // Notifications switched on from system settings while the flow is open settle a declined step.
    LaunchedEffect(permissionState.canDeliver) {
        viewModel.onEvent(OnboardingUiEvent.PermissionStateChanged(permissionState.canDeliver))
    }

    OnboardingContent(
        state = state,
        searchFieldState = viewModel.textFieldState,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

/**
 * The welcome flow: a pager of full-screen pages with a bottom bar. Moving between pages is presentation
 * mechanics and stays here; which pages exist, and finishing, are the ViewModel's.
 */
@Composable
internal fun OnboardingContent(
    state: OnboardingUiState,
    searchFieldState: TextFieldState,
    onEvent: (OnboardingUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    when (val contentState = state.contentState) {
        OnboardingContentState.Loading -> LoadingPage(modifier = modifier)
        is OnboardingContentState.Ready -> OnboardingPager(
            pages = contentState.pages,
            state = state,
            searchFieldState = searchFieldState,
            onEvent = onEvent,
            modifier = modifier
        )
    }
}

@Composable
private fun OnboardingPager(
    pages: List<OnboardingPage>,
    state: OnboardingUiState,
    searchFieldState: TextFieldState,
    onEvent: (OnboardingUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // The platforms page has a text field; without this the keyboard would stay up over the next page.
    LaunchedEffect(pagerState.currentPage) { focusManager.clearFocus() }

    fun scrollToPage(page: Int) {
        scope.launch { pagerState.animateScrollToPage(page) }
    }

    // On the first page back leaves the flow like on any root screen; from then on it goes one page back.
    BackHandler(enabled = pagerState.currentPage > 0) {
        scrollToPage(pagerState.currentPage - 1)
    }

    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        // Skip is on every page, the last one included, because FR-004 promises a way out from anywhere.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.large),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { onEvent(OnboardingUiEvent.SkipClicked) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.appColors.textOnSurface)
            ) {
                Text(text = stringResource(Res.string.onboarding_skip))
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { index ->
            when (val page = pages[index]) {
                OnboardingPage.Platforms -> OnboardingPlatformsPage(
                    pickerState = state.platformPicker,
                    selectedCount = state.selectedPlatformCount,
                    searchFieldState = searchFieldState,
                    onToggle = { platformId -> onEvent(OnboardingUiEvent.PlatformToggled(platformId)) },
                    onClearQuery = { onEvent(OnboardingUiEvent.ClearPlatformQuery) },
                    onRetry = { onEvent(OnboardingUiEvent.RetryPlatformSync) }
                )

                OnboardingPage.Reminders -> OnboardingRemindersPage(
                    step = state.reminderStep,
                    onAllowClick = { onEvent(OnboardingUiEvent.AllowNotificationsClicked) },
                    onNotNowClick = { onEvent(OnboardingUiEvent.NotNowClicked) }
                )

                else -> page.toInfoUiModel()?.let { OnboardingInfoPage(page = it) }
            }
        }
        OnboardingBottomBar(
            pagerState = pagerState,
            onBack = { scrollToPage(pagerState.currentPage - 1) },
            onNext = { scrollToPage(pagerState.currentPage + 1) },
            onFinish = { onEvent(OnboardingUiEvent.FinishClicked) }
        )
    }
}

@QuestLogPreviews
@Composable
private fun OnboardingContentLoadingPreview() {
    QuestLogTheme {
        OnboardingContent(state = OnboardingUiState(), searchFieldState = TextFieldState(), onEvent = {})
    }
}

@QuestLogPreviews
@Composable
private fun OnboardingContentReadyPreview() {
    QuestLogTheme {
        OnboardingContent(
            state = OnboardingUiState(
                contentState = OnboardingContentState.Ready(
                    listOf(
                        OnboardingPage.Welcome,
                        OnboardingPage.Discover,
                        OnboardingPage.Lists,
                        OnboardingPage.Radar,
                        OnboardingPage.Platforms,
                        OnboardingPage.Reminders
                    )
                )
            ),
            searchFieldState = TextFieldState(),
            onEvent = {}
        )
    }
}
