package com.nikolasguillen.questlog.core.domain.di

import com.nikolasguillen.questlog.core.domain.radar.GetRadarTimelineUseCase
import com.nikolasguillen.questlog.core.domain.radar.RefreshReleaseDatesUseCase
import com.nikolasguillen.questlog.core.domain.usecase.GetGameDetailUseCase
import com.nikolasguillen.questlog.core.domain.usecase.GetSavedGamesUseCase
import com.nikolasguillen.questlog.core.domain.usecase.RefreshGameDetailUseCase
import com.nikolasguillen.questlog.core.domain.usecase.SetGameStatusUseCase
import com.nikolasguillen.questlog.core.domain.usecase.ToggleWishlistUseCase
import com.nikolasguillen.questlog.core.domain.usecase.UpdateGameUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.GetDiscoverFeedUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.GetKnownPlatformsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.GetSelectedPlatformIdsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.GetSelectedPlatformsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.GetTasteProfileUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.SyncPlatformCatalogUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.ToggleOwnedPlatformUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.AddGameToListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.CreateListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.DeleteListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.GetListsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.GetWishlistAssignmentsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.GetWishlistDetailUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.GetWishlistViewModeUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.GetWishlistedGameIdsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.RemoveGameFromListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.SetDefaultListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.SetWishlistViewModeUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.UpdateListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.DeliverReleaseNotificationUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.GetReleaseNotificationGameIdsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.SetReleaseNotificationEnabledUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.SyncReleaseNotificationsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.AddSearchToHistoryUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.ClearAllHistoryUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.ClearRecentGamesUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.DeleteSearchHistoryItemUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.GetRecentSearchActivityUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.GetSearchSuggestionsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.RemoveRecentGameUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.SearchGamesUseCase
import com.nikolasguillen.questlog.core.domain.usecase.settings.CompleteOnboardingUseCase
import com.nikolasguillen.questlog.core.domain.usecase.settings.GetAppearanceModeUseCase
import com.nikolasguillen.questlog.core.domain.usecase.settings.GetOnboardingCompletedUseCase
import com.nikolasguillen.questlog.core.domain.usecase.settings.SetAppearanceModeUseCase
import com.nikolasguillen.questlog.core.domain.usecase.translation.DownloadTranslationModelUseCase
import com.nikolasguillen.questlog.core.domain.usecase.translation.GetTranslationModelStatusUseCase
import com.nikolasguillen.questlog.core.domain.usecase.translation.TranslateGameDescriptionUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/** Every use case and domain service is a plain class; a fresh instance per request is enough for all of them. */
val domainModule = module {
    factoryOf(::GetRadarTimelineUseCase)
    factoryOf(::RefreshReleaseDatesUseCase)
    factoryOf(::GetGameDetailUseCase)
    factoryOf(::GetSavedGamesUseCase)
    factoryOf(::RefreshGameDetailUseCase)
    factoryOf(::SetGameStatusUseCase)
    factoryOf(::ToggleWishlistUseCase)
    factoryOf(::UpdateGameUseCase)
    factoryOf(::GetDiscoverFeedUseCase)
    factoryOf(::GetKnownPlatformsUseCase)
    factoryOf(::GetSelectedPlatformIdsUseCase)
    factoryOf(::GetSelectedPlatformsUseCase)
    factoryOf(::GetTasteProfileUseCase)
    factoryOf(::SyncPlatformCatalogUseCase)
    factoryOf(::ToggleOwnedPlatformUseCase)
    factoryOf(::AddGameToListUseCase)
    factoryOf(::CreateListUseCase)
    factoryOf(::DeleteListUseCase)
    factoryOf(::GetListsUseCase)
    factoryOf(::GetWishlistAssignmentsUseCase)
    factoryOf(::GetWishlistDetailUseCase)
    factoryOf(::GetWishlistViewModeUseCase)
    factoryOf(::GetWishlistedGameIdsUseCase)
    factoryOf(::RemoveGameFromListUseCase)
    factoryOf(::SetDefaultListUseCase)
    factoryOf(::SetWishlistViewModeUseCase)
    factoryOf(::UpdateListUseCase)
    factoryOf(::DeliverReleaseNotificationUseCase)
    factoryOf(::GetReleaseNotificationGameIdsUseCase)
    factoryOf(::SetReleaseNotificationEnabledUseCase)
    factoryOf(::SyncReleaseNotificationsUseCase)
    factoryOf(::AddSearchToHistoryUseCase)
    factoryOf(::ClearAllHistoryUseCase)
    factoryOf(::ClearRecentGamesUseCase)
    factoryOf(::DeleteSearchHistoryItemUseCase)
    factoryOf(::GetRecentSearchActivityUseCase)
    factoryOf(::GetSearchSuggestionsUseCase)
    factoryOf(::RemoveRecentGameUseCase)
    factoryOf(::SearchGamesUseCase)
    factoryOf(::CompleteOnboardingUseCase)
    factoryOf(::GetAppearanceModeUseCase)
    factoryOf(::GetOnboardingCompletedUseCase)
    factoryOf(::SetAppearanceModeUseCase)
    factoryOf(::DownloadTranslationModelUseCase)
    factoryOf(::GetTranslationModelStatusUseCase)
    factoryOf(::TranslateGameDescriptionUseCase)
}
