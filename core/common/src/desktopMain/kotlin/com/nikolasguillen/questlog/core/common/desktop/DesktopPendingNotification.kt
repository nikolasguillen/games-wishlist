package com.nikolasguillen.questlog.core.common.desktop

import kotlin.time.Instant

/** A notification of ours that the system still has to deliver. [fireAt] is `null` for one that is shown at once. */
class DesktopPendingNotification(val gameId: Int, val fireAt: Instant?)
