package com.nikolasguillen.questlog.core.common.desktop.windows

import com.sun.jna.WString
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import java.io.File

internal const val APP_USER_MODEL_ID = "com.nikolasguillen.questlog"

private const val RESOURCES_DIRECTORY_PROPERTY = "compose.application.resources.dir"
private const val ICON_FILE_NAME = "notification.png"
private const val AUMID_KEY = "Software\\Classes\\AppUserModelId\\$APP_USER_MODEL_ID"

/**
 * The identity Windows needs to show a toast for an unpackaged app: a per-user registry entry with the app's name and
 * icon, and the same id on the process. It needs no Start menu shortcut, which jpackage cannot give an id to. It is
 * written at every launch, so a moved or reinstalled app keeps it right, and it is safe to repeat.
 *
 * Nothing here is fatal: without the entry Windows shows the toast under a generic name, or refuses it, and the
 * notification layer then reports the failure.
 */
internal object AumidRegistration {

    fun register() {
        runCatching { writeRegistryEntry() }
        runCatching { Shell32.instance.SetCurrentProcessExplicitAppUserModelID(WString(APP_USER_MODEL_ID)) }
    }

    private fun writeRegistryEntry() {
        val root = WinReg.HKEY_CURRENT_USER
        if (!Advapi32Util.registryKeyExists(root, AUMID_KEY)) Advapi32Util.registryCreateKey(root, AUMID_KEY)
        Advapi32Util.registrySetStringValue(root, AUMID_KEY, "DisplayName", "QuestLog")
        iconFile()?.let { Advapi32Util.registrySetStringValue(root, AUMID_KEY, "IconUri", it.absolutePath) }
    }

    private fun iconFile(): File? =
        System.getProperty(RESOURCES_DIRECTORY_PROPERTY)?.let { File(it, ICON_FILE_NAME) }?.takeIf { it.isFile }
}
