package com.nikolasguillen.questlog.desktop

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.Taskbar
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

private const val ICON_RESOURCE = "questlog-icon.png"

/** The QuestLog artwork at 256 px, or `null` when the resource is missing or unreadable. */
private val appIconImage: BufferedImage? by lazy {
    Thread.currentThread().contextClassLoader?.getResourceAsStream(ICON_RESOURCE)?.use { ImageIO.read(it) }
}

/** The window icon: what the Windows taskbar and title bar show while the app runs. */
internal val appIconPainter: BitmapPainter? by lazy {
    appIconImage?.let { BitmapPainter(it.toComposeImageBitmap()) }
}

/**
 * Puts the artwork in the macOS Dock. The Dock icon of a process started with `run` or `runDistributable` ignores the
 * window icon, and the installed copy takes it from the bundle instead.
 */
internal fun installDockIcon() {
    val icon = appIconImage ?: return
    if (!Taskbar.isTaskbarSupported()) return
    val taskbar = Taskbar.getTaskbar()
    if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) return
    // The icon is cosmetic: a refused call must not stop the app from starting.
    runCatching { taskbar.iconImage = icon }
}
