package com.nikolasguillen.questlog.core.cloud

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Keeps the Activity that is on screen. Both provider sign-in flows need one to present their UI, and no contract
 * signature may carry an Activity, so the flows read it from here.
 */
internal class CurrentActivityTracker : Application.ActivityLifecycleCallbacks {
    private var resumed: WeakReference<Activity>? = null

    /** The resumed Activity, or `null` when the app is in the background or the Activity is gone. */
    val current: Activity? get() = resumed?.get()

    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumed?.get() === activity) resumed = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
