package com.nikolasguillen.questlog.core.ui.util

import com.nikolasguillen.questlog.core.common.desktop.DesktopNotificationStatus
import com.nikolasguillen.questlog.core.common.desktop.DesktopNotificationStatus.AUTHORIZED
import com.nikolasguillen.questlog.core.common.desktop.DesktopNotificationStatus.DENIED
import com.nikolasguillen.questlog.core.common.desktop.DesktopNotificationStatus.NOT_DETERMINED
import com.nikolasguillen.questlog.core.common.desktop.DesktopNotificationStatus.UNSUPPORTED
import org.junit.Assert.assertEquals
import org.junit.Test

class DesktopPermissionMappingTest {

    private fun state(status: DesktopNotificationStatus, isWindows: Boolean) =
        status.toPermissionState(isWindows, request = {}).let {
            Triple(it.canDeliver, it.isPermanentlyDenied, it.requiresRuntimePermission)
        }

    @Test
    fun `windows delivers only when authorized and has no runtime permission`() {
        assertEquals(Triple(true, false, false), state(AUTHORIZED, isWindows = true))
    }

    @Test
    fun `windows treats every state but authorized as blocked`() {
        listOf(NOT_DETERMINED, DENIED, UNSUPPORTED).forEach {
            assertEquals(it.name, Triple(false, true, false), state(it, isWindows = true))
        }
    }

    @Test
    fun `macos delivers when authorized and asks at runtime`() {
        assertEquals(Triple(true, false, true), state(AUTHORIZED, isWindows = false))
    }

    @Test
    fun `macos is not blocked while undecided so the prompt can still appear`() {
        assertEquals(Triple(false, false, true), state(NOT_DETERMINED, isWindows = false))
    }

    @Test
    fun `macos is blocked when denied or unsupported`() {
        listOf(DENIED, UNSUPPORTED).forEach {
            assertEquals(it.name, Triple(false, true, true), state(it, isWindows = false))
        }
    }
}
