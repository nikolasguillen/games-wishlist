import FirebaseCore
import QuestLogShared
import UIKit

/// Starts Firebase, the app's Kotlin side and the background task, on every launch.
///
/// The system can launch the app in the background to run the release refresh. Then no scene is created and
/// `ContentView` is never built, so neither can be where this happens. Firebase starts first, because the sign-in
/// providers and the notification delegate may need it at launch; registration comes second and Kotlin start third,
/// all before this method returns: the system calls the task's launch handler only after launch has finished, so
/// Koin is always up by then.
final class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        ReleaseRefreshTask.register()
        QuestLogIosKt.startQuestLog(
            languageModelBridge: FoundationModelsBridge(),
            appRefreshTaskBridge: ReleaseRefreshTask(),
            identityProviderBridge: IdentityProviderBridge()
        )
        return true
    }
}
