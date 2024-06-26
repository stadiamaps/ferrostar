import SwiftUI

struct FerrostarFormatterKey: EnvironmentKey {
    static var defaultValue: any FerrostarFormatters = DefaultFerrostarFormatters()
}

public extension EnvironmentValues {
    var ferrostarFormatters: any FerrostarFormatters {
        get { self[FerrostarFormatterKey.self] }
        set { self[FerrostarFormatterKey.self] = newValue }
    }
}

public extension View {
    /// Apply the ferrostar formatters to the view stack below.
    ///
    /// This controls all value formatters for subviews in the ferrostar navigation view stack.
    ///
    /// - Parameter formatters: The ferrostar formatters to use for any value formatter below..
    /// - Returns: the modified view.
    func ferrostarFormatters(_ formatters: any FerrostarFormatters) -> some View {
        environment(\.ferrostarFormatters, formatters)
    }
}
