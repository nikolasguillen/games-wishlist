package com.nikolasguillen.questlog.di

import com.nikolasguillen.questlog.feature.gamedetail.GameDetailViewModel
import com.nikolasguillen.questlog.feature.lists.ListsViewModel
import com.nikolasguillen.questlog.feature.onboarding.OnboardingViewModel
import com.nikolasguillen.questlog.feature.radar.RadarViewModel
import com.nikolasguillen.questlog.feature.search.SearchViewModel
import com.nikolasguillen.questlog.feature.settings.OwnedPlatformsViewModel
import com.nikolasguillen.questlog.feature.settings.ReleaseNotificationsViewModel
import com.nikolasguillen.questlog.feature.settings.SettingsViewModel
import com.nikolasguillen.questlog.feature.wishlist.WishlistViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Feature modules own no DI module, so every ViewModel is registered here.
 *
 * [GameDetailViewModel] and [WishlistViewModel] take their route argument (a game id, a list id) as their
 * first constructor parameter; the caller supplies it with `koinViewModel { parametersOf(id) }`.
 */
val viewModelModule = module {
    viewModelOf(::SearchViewModel)
    viewModelOf(::RadarViewModel)
    viewModelOf(::ListsViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::OwnedPlatformsViewModel)
    viewModelOf(::ReleaseNotificationsViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::GameDetailViewModel)
    viewModelOf(::WishlistViewModel)
}
