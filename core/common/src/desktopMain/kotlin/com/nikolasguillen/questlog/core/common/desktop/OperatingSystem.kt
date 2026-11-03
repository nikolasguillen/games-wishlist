package com.nikolasguillen.questlog.core.common.desktop

/** Whether the desktop app runs on macOS. */
fun isMacOs(): Boolean = System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)

/** Whether the desktop app runs on Windows. */
fun isWindows(): Boolean = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)
