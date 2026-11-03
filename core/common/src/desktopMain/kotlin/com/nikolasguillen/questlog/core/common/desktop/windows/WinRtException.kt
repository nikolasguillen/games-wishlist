package com.nikolasguillen.questlog.core.common.desktop.windows

/** A WinRT or COM call that returned a failing `HRESULT`. */
internal class WinRtException(val hresult: Int, what: String) :
    RuntimeException("$what failed: HRESULT 0x${hresult.toUInt().toString(16)}")
