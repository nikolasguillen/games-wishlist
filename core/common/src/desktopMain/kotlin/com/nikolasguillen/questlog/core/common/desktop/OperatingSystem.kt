package com.nikolasguillen.questlog.core.common.desktop

/** Whether the desktop app runs on macOS. The only other desktop OS it supports is Windows. */
fun isMacOs(): Boolean = System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)
