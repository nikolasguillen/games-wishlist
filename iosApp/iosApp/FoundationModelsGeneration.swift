import QuestLogShared

/// Cancels a running generation when Kotlin cancels the coroutine waiting on it. `task` is nil when nothing
/// was started (below iOS 26).
final class FoundationModelsGeneration: NSObject, DataAppleLanguageModelGeneration {
    private let task: Task<Void, Never>?

    init(task: Task<Void, Never>?) {
        self.task = task
        super.init()
    }

    func cancel() {
        task?.cancel()
    }
}
