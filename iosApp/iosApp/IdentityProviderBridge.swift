import AuthenticationServices
import CryptoKit
import FirebaseAuth
import FirebaseCore
import Foundation
import GoogleSignIn
import QuestLogShared
import UIKit
import os

/// Google and Apple sign-in behind the Kotlin `CloudIdentityProviderBridge`.
///
/// GoogleSignIn-iOS and AuthenticationServices cannot be imported from Kotlin/Native, so the Kotlin side reaches them
/// through this class, which `AppDelegate` hands to `startQuestLog`. Swift owns everything about presentation: the
/// view controller the sheets come from, the Apple nonce and the Google client id. Each `onResult` is called exactly
/// once. No token is ever logged.
final class IdentityProviderBridge: NSObject, CloudIdentityProviderBridge {

    private static let logger = Logger(subsystem: "com.nikolasguillen.questlog", category: "sign-in")

    /// The Apple sign-in in flight. `ASAuthorizationController` keeps its delegate weakly, so it lives here.
    private var appleSignIn: AppleSignIn?

    func signInWithGoogle(onResult: @escaping (CloudGoogleSignInResult) -> Void) {
        DispatchQueue.main.async {
            guard let clientID = FirebaseApp.app()?.options.clientID,
                  let presenter = Self.topViewController() else {
                Self.logger.error("Google sign-in cannot start: no client id or no view controller")
                onResult(CloudGoogleSignInResult(outcome: .failed, idToken: nil, accessToken: nil))
                return
            }
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
            GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
                if let error = error as NSError? {
                    let cancelled = error.domain == kGIDSignInErrorDomain
                        && error.code == GIDSignInError.canceled.rawValue
                    if !cancelled {
                        Self.logger.error("Google sign-in failed: \(error.domain, privacy: .public) \(error.code)")
                    }
                    onResult(CloudGoogleSignInResult(
                        outcome: cancelled ? .cancelled : .failed, idToken: nil, accessToken: nil
                    ))
                    return
                }
                guard let user = result?.user, let idToken = user.idToken?.tokenString else {
                    onResult(CloudGoogleSignInResult(outcome: .failed, idToken: nil, accessToken: nil))
                    return
                }
                onResult(CloudGoogleSignInResult(
                    outcome: .success, idToken: idToken, accessToken: user.accessToken.tokenString
                ))
            }
        }
    }

    func signInWithApple(onResult: @escaping (CloudAppleSignInResult) -> Void) {
        DispatchQueue.main.async {
            guard let presenter = Self.topViewController() else {
                Self.logger.error("Apple sign-in cannot start: no view controller")
                onResult(CloudAppleSignInResult(outcome: .failed, idToken: nil, rawNonce: nil, authorizationCode: nil))
                return
            }
            let signIn = AppleSignIn(presenter: presenter) { [weak self] result in
                self?.appleSignIn = nil
                onResult(result)
            }
            self.appleSignIn = signIn
            signIn.start()
        }
    }

    func revokeAppleToken(authorizationCode: String, onResult: @escaping (KotlinBoolean) -> Void) {
        Auth.auth().revokeToken(withAuthorizationCode: authorizationCode) { error in
            if let error = error {
                Self.logger.error("Apple token revocation failed: \((error as NSError).code)")
            }
            onResult(KotlinBoolean(bool: error == nil))
        }
    }

    func signOutGoogle() {
        GIDSignIn.sharedInstance.signOut()
    }

    /// The view controller a sign-in sheet is presented from: the topmost one of the active window scene.
    private static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        var top = scene?.windows.first(where: \.isKeyWindow)?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}

/// One run of Sign in with Apple: the request with its nonce, the system sheet and the outcome.
private final class AppleSignIn: NSObject, ASAuthorizationControllerDelegate,
    ASAuthorizationControllerPresentationContextProviding {

    private let presenter: UIViewController
    private let onResult: (CloudAppleSignInResult) -> Void
    private let rawNonce = AppleSignIn.randomNonce()

    init(presenter: UIViewController, onResult: @escaping (CloudAppleSignInResult) -> Void) {
        self.presenter = presenter
        self.onResult = onResult
    }

    func start() {
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        // Apple gets the hash and Firebase gets the raw value, so a stolen identity token cannot be replayed.
        request.nonce = AppleSignIn.sha256(rawNonce)
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        presenter.view.window ?? ASPresentationAnchor()
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let tokenData = credential.identityToken,
              let idToken = String(data: tokenData, encoding: .utf8),
              let codeData = credential.authorizationCode,
              let code = String(data: codeData, encoding: .utf8) else {
            onResult(CloudAppleSignInResult(outcome: .failed, idToken: nil, rawNonce: nil, authorizationCode: nil))
            return
        }
        onResult(CloudAppleSignInResult(
            outcome: .success, idToken: idToken, rawNonce: rawNonce, authorizationCode: code
        ))
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        let cancelled = (error as? ASAuthorizationError)?.code == .canceled
        onResult(CloudAppleSignInResult(
            outcome: cancelled ? .cancelled : .failed, idToken: nil, rawNonce: nil, authorizationCode: nil
        ))
    }

    /// 32 characters from a cryptographically secure source.
    private static func randomNonce(length: Int = 32) -> String {
        let charset = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._")
        var result = ""
        while result.count < length {
            var bytes = [UInt8](repeating: 0, count: 16)
            let status = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
            precondition(status == errSecSuccess, "Unable to generate a nonce")
            for byte in bytes where result.count < length && byte < charset.count * (256 / charset.count) {
                result.append(charset[Int(byte) % charset.count])
            }
        }
        return result
    }

    private static func sha256(_ input: String) -> String {
        SHA256.hash(data: Data(input.utf8)).map { String(format: "%02x", $0) }.joined()
    }
}
