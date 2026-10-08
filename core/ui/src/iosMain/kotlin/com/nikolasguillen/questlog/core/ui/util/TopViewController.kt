package com.nikolasguillen.questlog.core.ui.util

import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

/**
 * The view controller currently on top of the key window, which is what a system sheet (the image picker, the share
 * sheet) has to be presented from. `null` when there is no window yet.
 */
internal fun topViewController(): UIViewController? {
    val keyWindow = UIApplication.sharedApplication.connectedScenes
        .filterIsInstance<UIWindowScene>()
        .flatMap { scene -> scene.windows.filterIsInstance<UIWindow>() }
        .firstOrNull { it.isKeyWindow() }
    var top = keyWindow?.rootViewController
    while (top?.presentedViewController != null) {
        top = top.presentedViewController
    }
    return top
}
