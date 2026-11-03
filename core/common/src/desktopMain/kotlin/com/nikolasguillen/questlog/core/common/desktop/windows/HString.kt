package com.nikolasguillen.questlog.core.common.desktop.windows

import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference

/** A WinRT `HSTRING` made from [text]. Close it to free the handle. */
internal class HString(text: String) : AutoCloseable {

    val handle: Pointer

    init {
        val out = PointerByReference()
        checkHresult(Combase.instance.WindowsCreateString(WString(text), text.length, out), "WindowsCreateString")
        handle = out.value
    }

    override fun close() {
        Combase.instance.WindowsDeleteString(handle)
    }

    companion object {

        /** Reads an `HSTRING` that a call returned, then frees it: the caller owns a returned string. */
        fun readAndFree(handle: Pointer?): String {
            if (handle == null) return ""
            try {
                val length = IntByReference()
                val buffer = Combase.instance.WindowsGetStringRawBuffer(handle, length) ?: return ""
                return String(buffer.getCharArray(0, length.value))
            } finally {
                Combase.instance.WindowsDeleteString(handle)
            }
        }
    }
}
