import os

public extension Logger {
    /// For more information see:
    /// - https://developer.apple.com/documentation/os/viewing-log-messages
    /// For even more details see:
    /// - https://developer.apple.com/forums/thread/705868
    init(category: String) {
        self.init(subsystem: "Ferrostar", category: category)
    }
}
