package com.nikolasguillen.questlog.core.common.desktop.windows

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.WString

/** The one function of `shell32.dll` the notifications need. */
@Suppress("FunctionName") // The name is the Win32 symbol.
internal interface Shell32 : Library {

    fun SetCurrentProcessExplicitAppUserModelID(appId: WString): Int

    companion object {
        val instance: Shell32 by lazy { Native.load("shell32", Shell32::class.java) }
    }
}
