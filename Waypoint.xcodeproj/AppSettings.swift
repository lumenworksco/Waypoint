import Foundation

final class AppSettings {
    static let shared = AppSettings()
    private init() {}

    private let didShowLongPressTipKey = "didShowLongPressTipKey"

    var didShowLongPressTip: Bool {
        get { UserDefaults.standard.bool(forKey: didShowLongPressTipKey) }
        set { UserDefaults.standard.set(newValue, forKey: didShowLongPressTipKey) }
    }
}
