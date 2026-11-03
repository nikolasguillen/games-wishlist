package com.nikolasguillen.questlog.core.common.desktop.windows

import com.sun.jna.Function
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Guid
import com.sun.jna.ptr.PointerByReference

private const val QUERY_INTERFACE_SLOT = 0
private const val RELEASE_SLOT = 2

/** The first method slot after `IUnknown` and `IInspectable` (GetIids, GetRuntimeClassName, GetTrustLevel). */
internal const val FIRST_WINRT_SLOT = 6

internal fun checkHresult(hresult: Int, what: String) {
    if (hresult < 0) throw WinRtException(hresult, what)
}

/**
 * A COM interface pointer, called through its vtable: slot `n` is the `n`th function pointer of the table the object
 * starts with. Close it to `Release` the reference. Only the 64-bit calling convention is exercised, which is the one
 * the installers target.
 */
internal class ComPtr(val pointer: Pointer) : AutoCloseable {

    /** Calls vtable slot [slot] with this pointer as the implicit first argument and returns its `HRESULT`. */
    fun call(slot: Int, vararg arguments: Any?): Int {
        val vtable = pointer.getPointer(0)
        val function = Function.getFunction(
            vtable.getPointer((slot * Native.POINTER_SIZE).toLong()),
            Function.ALT_CONVENTION
        )
        return function.invokeInt(arrayOf<Any?>(pointer, *arguments))
    }

    /** Calls [slot] and checks the result, naming the call in the error. */
    fun invoke(what: String, slot: Int, vararg arguments: Any?) = checkHresult(call(slot, *arguments), what)

    /** Calls a slot that fills a pointer out parameter, and wraps the pointer. */
    fun invokeForPointer(what: String, slot: Int, vararg arguments: Any?): ComPtr {
        val out = PointerByReference()
        invoke(what, slot, *arguments, out)
        return ComPtr(out.value)
    }

    fun queryInterface(iid: String): ComPtr {
        val out = PointerByReference()
        checkHresult(call(QUERY_INTERFACE_SLOT, Guid.IID(iid), out), "QueryInterface")
        return ComPtr(out.value)
    }

    override fun close() {
        call(RELEASE_SLOT)
    }
}
