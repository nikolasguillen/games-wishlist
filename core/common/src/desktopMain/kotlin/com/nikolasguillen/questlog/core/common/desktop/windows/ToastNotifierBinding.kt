package com.nikolasguillen.questlog.core.common.desktop.windows

import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.LongByReference
import com.sun.jna.ptr.PointerByReference

/**
 * Typed calls over `Windows.UI.Notifications` for one notifier, made through COM vtables. The interface ids and slot
 * numbers are the ones of the Windows SDK's `windows.ui.notifications.h` and `windows.data.xml.dom.h`.
 *
 * Every call has to run on the thread that called `RoInitialize`.
 */
internal class ToastNotifierBinding(applicationId: String) : AutoCloseable {

    private val notifier: ComPtr = HString("Windows.UI.Notifications.ToastNotificationManager").use { className ->
        activationFactory(className, IID_TOAST_NOTIFICATION_MANAGER_STATICS).use { statics ->
            HString(applicationId).use { id ->
                // IToastNotificationManagerStatics::CreateToastNotifierWithId
                statics.invokeForPointer("CreateToastNotifierWithId", FIRST_WINRT_SLOT + 1, id.handle)
            }
        }
    }

    /** A scheduled toast as the notifier holds it. */
    class Scheduled(val id: String, val ticks: Long)

    /** `NotificationSetting`: `0` enabled, anything else is a reason notifications are off. */
    fun setting(): Int {
        val out = IntByReference()
        // IToastNotifier::get_Setting
        notifier.invoke("get_Setting", FIRST_WINRT_SLOT + 2, out)
        return out.value
    }

    /** Arms a toast for [deliveryTicks] (WinRT `DateTime`) under [toastId]. */
    fun schedule(toastId: String, xml: String, deliveryTicks: Long) {
        loadXml(xml).use { document ->
            HString("Windows.UI.Notifications.ScheduledToastNotification").use { className ->
                activationFactory(className, IID_SCHEDULED_TOAST_NOTIFICATION_FACTORY).use { factory ->
                    // IScheduledToastNotificationFactory::CreateScheduledToastNotification(xml, DateTime)
                    factory.invokeForPointer(
                        "CreateScheduledToastNotification", FIRST_WINRT_SLOT, document.pointer, deliveryTicks
                    ).use { toast ->
                        HString(toastId).use { id ->
                            // IScheduledToastNotification::put_Id
                            toast.invoke("put_Id", FIRST_WINRT_SLOT + 4, id.handle)
                        }
                        // The slot numbers are from memory of the SDK header: a toast that does not report the id it
                        // was given means the table is not what is assumed, and nothing should be armed from it.
                        check(idOf(toast) == toastId) { "Scheduled toast id slot mismatch" }
                        // IToastNotifier::AddToSchedule
                        notifier.invoke("AddToSchedule", FIRST_WINRT_SLOT + 3, toast.pointer)
                    }
                }
            }
        }
    }

    /** Shows a toast now; it is not pending afterwards. */
    fun show(xml: String) {
        loadXml(xml).use { document ->
            HString("Windows.UI.Notifications.ToastNotification").use { className ->
                activationFactory(className, IID_TOAST_NOTIFICATION_FACTORY).use { factory ->
                    // IToastNotificationFactory::CreateToastNotification
                    factory.invokeForPointer("CreateToastNotification", FIRST_WINRT_SLOT, document.pointer).use { toast ->
                        // IToastNotifier::Show
                        notifier.invoke("Show", FIRST_WINRT_SLOT, toast.pointer)
                    }
                }
            }
        }
    }

    fun scheduled(): List<Scheduled> = forEachScheduled { toast ->
        val ticks = LongByReference()
        // IScheduledToastNotification::get_DeliveryTime
        toast.invoke("get_DeliveryTime", FIRST_WINRT_SLOT + 1, ticks)
        Scheduled(idOf(toast), ticks.value)
    }

    /** Removes every scheduled toast whose id is in [toastIds]. */
    fun remove(toastIds: Set<String>) {
        forEachScheduled { toast ->
            // IToastNotifier::RemoveFromSchedule
            if (idOf(toast) in toastIds) notifier.invoke("RemoveFromSchedule", FIRST_WINRT_SLOT + 4, toast.pointer)
        }
    }

    override fun close() {
        notifier.close()
    }

    private fun <T> forEachScheduled(action: (ComPtr) -> T): List<T> {
        // IToastNotifier::GetScheduledToastNotifications -> IVectorView<ScheduledToastNotification>
        notifier.invokeForPointer("GetScheduledToastNotifications", FIRST_WINRT_SLOT + 5).use { vector ->
            val size = IntByReference()
            // IVectorView::get_Size
            vector.invoke("get_Size", FIRST_WINRT_SLOT + 1, size)
            return (0 until size.value).map { index ->
                // IVectorView::GetAt
                vector.invokeForPointer("GetAt", FIRST_WINRT_SLOT, index).use(action)
            }
        }
    }

    private fun idOf(toast: ComPtr): String {
        val out = PointerByReference()
        // IScheduledToastNotification::get_Id
        toast.invoke("get_Id", FIRST_WINRT_SLOT + 5, out)
        return HString.readAndFree(out.value)
    }

    /** An `XmlDocument` holding [xml], as the `IXmlDocument` that the toast factories take. */
    private fun loadXml(xml: String): ComPtr {
        val instance = PointerByReference()
        HString("Windows.Data.Xml.Dom.XmlDocument").use { className ->
            checkHresult(Combase.instance.RoActivateInstance(className.handle, instance), "RoActivateInstance")
        }
        return ComPtr(instance.value).use { inspectable ->
            inspectable.queryInterface(IID_XML_DOCUMENT_IO).use { io ->
                HString(xml).use { text -> io.invoke("LoadXml", FIRST_WINRT_SLOT, text.handle) }
            }
            inspectable.queryInterface(IID_XML_DOCUMENT)
        }
    }

    private companion object {
        const val IID_TOAST_NOTIFICATION_MANAGER_STATICS = "{50AC103F-D235-4598-BBEF-98FE4D1A3AD4}"
        const val IID_TOAST_NOTIFICATION_FACTORY = "{04124B20-82C6-4229-B109-FD9ED4662B53}"
        const val IID_SCHEDULED_TOAST_NOTIFICATION_FACTORY = "{E7BED191-0BB9-4189-8394-31761B476FD7}"
        const val IID_XML_DOCUMENT_IO = "{6CD0E74E-EE65-4489-9EBF-CA43E87BA637}"
        const val IID_XML_DOCUMENT = "{F7F3A506-1E87-42D6-BCFB-B8C809FA5494}"

        fun activationFactory(className: HString, iid: String): ComPtr {
            val factory = PointerByReference()
            checkHresult(
                Combase.instance.RoGetActivationFactory(className.handle, com.sun.jna.platform.win32.Guid.IID(iid), factory),
                "RoGetActivationFactory"
            )
            return ComPtr(factory.value)
        }
    }
}
