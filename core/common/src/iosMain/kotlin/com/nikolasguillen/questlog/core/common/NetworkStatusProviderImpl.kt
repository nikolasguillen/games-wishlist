package com.nikolasguillen.questlog.core.common

import kotlin.concurrent.AtomicInt
import platform.Network.nw_path_get_status
import platform.Network.nw_path_is_constrained
import platform.Network.nw_path_is_expensive
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

/**
 * The last network path the system reported. A path is "unmetered" when it is reachable and neither a cellular
 * "expensive" one nor in Low Data Mode, which is the closest iOS has to Android's `NET_CAPABILITY_NOT_METERED`.
 *
 * Until the monitor delivers its first path the answer is `false`: the one caller treats that as "do not start a
 * large download", the safe side.
 */
internal class NetworkStatusProviderImpl : NetworkStatusProvider {

    private val unmetered = AtomicInt(0)

    init {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path ->
            val usable = nw_path_get_status(path) == nw_path_status_satisfied &&
                !nw_path_is_expensive(path) &&
                !nw_path_is_constrained(path)
            unmetered.value = if (usable) 1 else 0
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
    }

    override val isUnmeteredNetworkAvailable: Boolean
        get() = unmetered.value == 1
}
