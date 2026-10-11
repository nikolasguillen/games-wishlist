import BackgroundTasks
import Foundation
import os
import QuestLogShared

/// The system's background app-refresh task (BackgroundTasks), behind the Kotlin `AppRefreshTaskBridge`.
///
/// Kotlin decides everything: whether a refresh is due, when the next request may start, what the outcome is.
/// This class only does what Kotlin cannot, which is to register the task, submit requests, and hand the outcome
/// and the expiration across. A rejected submit is logged and swallowed: background refresh may be switched off,
/// and the Simulator rejects every request. The refresh at launch still runs, so nothing is lost.
final class ReleaseRefreshTask: NSObject, DataAppRefreshTaskBridge {

    /// Must match `BGTaskSchedulerPermittedIdentifiers` in `Info.plist`.
    static let identifier = "com.nikolasguillen.questlog.release-dates-refresh"

    private static let logger = Logger(subsystem: "com.nikolasguillen.questlog", category: "background-refresh")

    /// Must run before `application(_:didFinishLaunchingWithOptions:)` returns: the system rejects a handler
    /// registered later.
    static func register() {
        let registered = BGTaskScheduler.shared.register(forTaskWithIdentifier: identifier, using: nil) { task in
            guard let task = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }
            handle(task)
        }
        if !registered {
            logger.error("Could not register the background refresh task")
        }
    }

    func submitRequest(earliestBeginEpochMillis: Int64) {
        let request = BGAppRefreshTaskRequest(identifier: Self.identifier)
        request.earliestBeginDate = Date(timeIntervalSince1970: TimeInterval(earliestBeginEpochMillis) / 1000)
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            Self.logger.notice("Background refresh request was not accepted: \(error.localizedDescription, privacy: .public)")
        }
    }

    /// The system launched the task. `setTaskCompleted` is required even after expiration, and Kotlin calls
    /// `onFinished` exactly once either way.
    private static func handle(_ task: BGAppRefreshTask) {
        let run = QuestLogIosKt.runAppRefreshTask { success in
            task.setTaskCompleted(success: success.boolValue)
        }
        task.expirationHandler = { run.cancel() }
    }

    #if DEBUG
    private static var debugRunHandle: DataAppRefreshTaskRun?

    /// Runs the task body without a `BGTask`, for the Simulator, which cannot launch background tasks. Call it
    /// from LLDB: `expr -l swift -- ReleaseRefreshTask.debugRun()`, or, where the Swift expression evaluator
    /// cannot see the app's module, `expr -l objc -- (void)[NSClassFromString(@"iosApp.ReleaseRefreshTask") debugRun]`
    /// (hence `@objc`).
    @objc static func debugRun() {
        logger.notice("debugRun started")
        debugRunHandle = QuestLogIosKt.runAppRefreshTask { success in
            logger.notice("debugRun finished, success: \(success.boolValue)")
        }
    }

    /// Ends the run `debugRun()` started, as the system's expiration does.
    @objc static func debugCancel() {
        logger.notice("debugCancel")
        debugRunHandle?.cancel()
    }
    #endif
}
