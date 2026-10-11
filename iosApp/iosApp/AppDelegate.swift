import QuestLogShared
import UIKit

/// Starts the app's Kotlin side and registers the background task, on every launch.
///
/// The system can launch the app in the background to run the release refresh. Then no scene is created and
/// `ContentView` is never built, so neither can be where this happens. Registration comes first and Kotlin start
/// second, both before this method returns: the system calls the task's launch handler only after launch has
/// finished, so Koin is always up by then.
final class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        ReleaseRefreshTask.register()
        QuestLogIosKt.startQuestLog(
            languageModelBridge: FoundationModelsBridge(),
            appRefreshTaskBridge: ReleaseRefreshTask()
        )
        return true
    }
}
