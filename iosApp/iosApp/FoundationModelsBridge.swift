import Foundation
import FoundationModels
import os
import QuestLogShared

/// Apple's on-device model (FoundationModels) behind the Kotlin `AppleLanguageModelBridge`.
///
/// Kotlin/Native cannot import FoundationModels, a Swift-only framework, so the Kotlin translator reaches it
/// through this class, which `ContentView` hands to `MainViewController`. FoundationModels is weak-linked
/// (`-weak_framework` in the target's linker flags) and every use is behind `#available(iOS 26.0, *)`, so the
/// app still launches on iOS 16–25, where this reports `.unavailable`.
final class FoundationModelsBridge: NSObject, DataAppleLanguageModelBridge {

    /// Every failure below reaches Kotlin as a plain `nil`, which the UI shows as "translation failed". The cause
    /// is logged here, never the text being translated, so a failure can be read from the console.
    private static let logger = Logger(subsystem: "com.nikolasguillen.questlog", category: "translation")

    func preferredLanguageTag() -> String {
        // Not Locale.current: its language follows the app's own localizations, and the app ships English only.
        Locale.preferredLanguages.first ?? "en"
    }

    func preferredLanguageEnglishName() -> String {
        let tag = preferredLanguageTag()
        return Locale(identifier: "en").localizedString(forIdentifier: tag) ?? tag
    }

    func availability() -> DataAppleLanguageModelAvailability {
        guard #available(iOS 26.0, *) else { return .unavailable }
        let model = Self.translationModel
        switch model.availability {
        case .available:
            // A backend that says "available" but has no context window cannot generate anything: the iOS 27
            // simulator on a macOS 26 host does exactly that, and fails every request with ModelManagerError 1026.
            if #available(iOS 26.4, *), model.contextSize == 0 {
                Self.logger.error("Model reports available but has a 0-token context window; treating it as unavailable")
                return .unavailable
            }
            return model.supportsLocale(Locale(identifier: preferredLanguageTag()))
                ? .available
                : .languageUnsupported
        case .unavailable(.modelNotReady):
            return .notReady
        case .unavailable:
            return .unavailable
        }
    }

    func generate(
        instructions: String,
        prompt: String,
        onResult: @escaping (String?) -> Void
    ) -> DataAppleLanguageModelGeneration {
        guard #available(iOS 26.0, *) else {
            onResult(nil)
            return FoundationModelsGeneration(task: nil)
        }
        let task = Task {
            onResult(await Self.respond(instructions: instructions, prompt: prompt))
        }
        return FoundationModelsGeneration(task: task)
    }

    /// Permissive guardrails: Apple's mode for transforming supplied text. Game descriptions are full of combat
    /// and violence that the default guardrails refuse to rewrite.
    @available(iOS 26.0, *)
    private static var translationModel: SystemLanguageModel {
        SystemLanguageModel(useCase: .general, guardrails: .permissiveContentTransformations)
    }

    /// A fresh session per call: a reused one accumulates its transcript into the context window.
    @available(iOS 26.0, *)
    private static func respond(instructions: String, prompt: String) async -> String? {
        let model = translationModel
        if #available(iOS 26.4, *) {
            // Half the window for the input, half left for the translation, which is about as long.
            if let tokens = try? await model.tokenCount(for: instructions + prompt), tokens > model.contextSize / 2 {
                logger.error("Refused: \(tokens, privacy: .public) tokens exceed half of the \(model.contextSize, privacy: .public)-token window")
                return nil
            }
        }
        do {
            let session = LanguageModelSession(model: model, instructions: instructions)
            return try await session.respond(to: prompt).content
        } catch {
            // Guardrails, context overflow, an unsupported language, cancellation: the caller shows the original.
            logger.error("Generation failed: \(String(describing: error), privacy: .public)")
            return nil
        }
    }
}
