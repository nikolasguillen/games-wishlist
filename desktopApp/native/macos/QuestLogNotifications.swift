// The macOS side of the desktop release reminders: a thin C ABI over UserNotifications, loaded by the JVM through
// JNA (specs/020-desktop-app, research R9; the ABI is in contracts/desktop-platform-contracts.md).
//
// UNUserNotificationCenter exists only inside an app bundle with a bundle identifier. Without one (a plain
// `./gradlew :desktopApp:run`) ql_authorization_status returns -1 and every other call does nothing, so the JVM
// reports UNSUPPORTED. Every callback is called exactly once, on any thread. The arrays handed to ql_pending's
// callback are valid only until it returns.

import Foundation
import UserNotifications

private let identifierPrefix = "release-reminder-"
private let gameIdKey = "gameId"

/// How long the system waits before it delivers a reminder that is already due.
private let immediateDelaySeconds = 1.0

/// How long a synchronous call waits for the system's answer before it gives up and reports "not determined".
private let answerTimeoutSeconds = 5.0

private var hasBundle: Bool { Bundle.main.bundleIdentifier != nil }

private func identifier(_ gameId: Int32) -> String { "\(identifierPrefix)\(gameId)" }

private func gameId(of identifier: String) -> Int32? {
    guard identifier.hasPrefix(identifierPrefix) else { return nil }
    return Int32(identifier.dropFirst(identifierPrefix.count))
}

/// Receives the click on a delivered reminder, and lets one show while the app is in front.
private final class NotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    var onClick: (@convention(c) (Int32) -> Void)?

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if let id = (response.notification.request.content.userInfo[gameIdKey] as? NSNumber)?.int32Value {
            onClick?(id)
        }
        completionHandler()
    }
}

// The center keeps only a weak reference to its delegate.
private let delegate = NotificationDelegate()

private func authorizationValue(_ status: UNAuthorizationStatus) -> Int32 {
    switch status {
    case .authorized, .provisional, .ephemeral: return 1
    case .denied: return 2
    default: return 0
    }
}

@_cdecl("ql_authorization_status")
public func ql_authorization_status() -> Int32 {
    guard hasBundle else { return -1 }
    let semaphore = DispatchSemaphore(value: 0)
    var value: Int32 = 0
    UNUserNotificationCenter.current().getNotificationSettings { settings in
        value = authorizationValue(settings.authorizationStatus)
        semaphore.signal()
    }
    _ = semaphore.wait(timeout: .now() + answerTimeoutSeconds)
    return value
}

@_cdecl("ql_request_authorization")
public func ql_request_authorization(_ done: @convention(c) (Int32) -> Void) {
    guard hasBundle else { return done(0) }
    UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { granted, _ in
        done(granted ? 1 : 0)
    }
}

private func add(
    id: Int32,
    title: UnsafePointer<CChar>,
    body: UnsafePointer<CChar>,
    trigger: UNNotificationTrigger,
    done: @escaping @convention(c) (Int32) -> Void
) {
    let content = UNMutableNotificationContent()
    content.title = String(cString: title)
    content.body = String(cString: body)
    content.sound = .default
    content.userInfo = [gameIdKey: NSNumber(value: id)]
    let request = UNNotificationRequest(identifier: identifier(id), content: content, trigger: trigger)
    UNUserNotificationCenter.current().add(request) { error in done(error == nil ? 1 : 0) }
}

@_cdecl("ql_add_calendar")
public func ql_add_calendar(
    _ id: Int32, _ title: UnsafePointer<CChar>, _ body: UnsafePointer<CChar>,
    _ year: Int32, _ month: Int32, _ day: Int32, _ hour: Int32, _ minute: Int32,
    _ done: @escaping @convention(c) (Int32) -> Void
) {
    guard hasBundle else { return done(0) }
    // Components with no time zone ("floating"): the 09:00 of the release day is the Mac's 09:00 wherever it is.
    var components = DateComponents()
    components.calendar = Calendar(identifier: .gregorian)
    components.year = Int(year)
    components.month = Int(month)
    components.day = Int(day)
    components.hour = Int(hour)
    components.minute = Int(minute)
    let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
    add(id: id, title: title, body: body, trigger: trigger, done: done)
}

@_cdecl("ql_add_immediate")
public func ql_add_immediate(
    _ id: Int32, _ title: UnsafePointer<CChar>, _ body: UnsafePointer<CChar>,
    _ done: @escaping @convention(c) (Int32) -> Void
) {
    guard hasBundle else { return done(0) }
    let trigger = UNTimeIntervalNotificationTrigger(timeInterval: immediateDelaySeconds, repeats: false)
    add(id: id, title: title, body: body, trigger: trigger, done: done)
}

@_cdecl("ql_pending")
public func ql_pending(_ done: @escaping @convention(c) (Int32, UnsafePointer<Int32>?, UnsafePointer<Double>?) -> Void) {
    guard hasBundle else { return done(0, nil, nil) }
    UNUserNotificationCenter.current().getPendingNotificationRequests { requests in
        var ids: [Int32] = []
        var seconds: [Double] = []
        for request in requests {
            guard let id = gameId(of: request.identifier) else { continue }
            ids.append(id)
            // NaN for a trigger without a calendar date: the one-second "already due" trigger.
            let next = (request.trigger as? UNCalendarNotificationTrigger)?.nextTriggerDate()
            seconds.append(next?.timeIntervalSince1970 ?? Double.nan)
        }
        ids.withUnsafeBufferPointer { idBuffer in
            seconds.withUnsafeBufferPointer { secondsBuffer in
                done(Int32(ids.count), idBuffer.baseAddress, secondsBuffer.baseAddress)
            }
        }
    }
}

@_cdecl("ql_remove")
public func ql_remove(_ ids: UnsafePointer<Int32>?, _ count: Int32) {
    guard hasBundle, let ids = ids, count > 0 else { return }
    let identifiers = (0..<Int(count)).map { identifier(ids[$0]) }
    UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: identifiers)
}

@_cdecl("ql_set_click_handler")
public func ql_set_click_handler(_ onClick: @escaping @convention(c) (Int32) -> Void) {
    guard hasBundle else { return }
    delegate.onClick = onClick
    UNUserNotificationCenter.current().delegate = delegate
}
