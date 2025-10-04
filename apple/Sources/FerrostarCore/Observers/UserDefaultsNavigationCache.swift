import FerrostarCoreFFI
import Foundation

public final class UserDefaultsNavigationCache: NavigationCache, @unchecked Sendable {
    private let userDefaults: UserDefaults
    private let key: String

    public init(
        _ userDefaults: UserDefaults = .standard,
        key: String = "ferrostar_cache"
    ) {
        self.userDefaults = userDefaults
        self.key = key
    }

    public func save(record: Data) {
        userDefaults.set(record, forKey: key)
    }

    public func load() -> Data? {
        userDefaults.data(forKey: key)
    }

    public func delete() {
        userDefaults.removeObject(forKey: key)
    }
}
