import SwiftUI

struct FerrostarThemeKey: EnvironmentKey {
    static var defaultValue: any FerrostarTheme = DefaultFerrostarTheme()
}

public extension EnvironmentValues {
    var ferrostarTheme: any FerrostarTheme {
        get { self[FerrostarThemeKey.self] }
        set { self[FerrostarThemeKey.self] = newValue }
    }
}

public extension View {
    /// Apply the ferrostar theme to the view stack below.
    ///
    /// - Parameter theme: The ferrostar theme to apply.
    /// - Returns: the modified view.
    func ferrostarTheme(_ theme: any FerrostarTheme) -> some View {
        environment(\.ferrostarTheme, theme)
    }
}
