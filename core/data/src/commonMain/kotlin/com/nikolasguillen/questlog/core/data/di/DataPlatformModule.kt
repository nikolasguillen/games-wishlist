package com.nikolasguillen.questlog.core.data.di

import org.koin.core.module.Module

/**
 * The bindings that differ per platform: where the settings `DataStore` lives, how release dates are refreshed
 * in the background, how reminders are scheduled and shown, the description translator, where cover images are
 * stored, and whether reminders are available at all.
 */
expect val dataPlatformModule: Module
