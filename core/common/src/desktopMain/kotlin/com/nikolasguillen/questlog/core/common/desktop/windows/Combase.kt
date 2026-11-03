package com.nikolasguillen.questlog.core.common.desktop.windows

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.Guid
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference

/** The Windows Runtime activation and string functions of `combase.dll`. Every function returns an `HRESULT`. */
@Suppress("FunctionName") // The names are the Win32 symbols.
internal interface Combase : Library {

    fun RoInitialize(initType: Int): Int

    fun RoGetActivationFactory(classId: Pointer, iid: Guid.GUID, factory: PointerByReference): Int

    fun RoActivateInstance(classId: Pointer, instance: PointerByReference): Int

    fun WindowsCreateString(source: WString, length: Int, string: PointerByReference): Int

    fun WindowsDeleteString(string: Pointer?): Int

    fun WindowsGetStringRawBuffer(string: Pointer?, length: IntByReference?): Pointer?

    companion object {
        const val RO_INIT_MULTITHREADED = 1

        val instance: Combase by lazy { Native.load("combase", Combase::class.java) }
    }
}
