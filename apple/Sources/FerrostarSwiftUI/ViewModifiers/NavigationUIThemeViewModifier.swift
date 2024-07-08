import SwiftUI

struct NavigationUIThemeKey: EnvironmentKey {
    static var defaultValue: any NavigationUITheme = DefaultNavigationUITheme()
}

public extension EnvironmentValues {
    var navigationUITheme: any NavigationUITheme {
        get { self[NavigationUIThemeKey.self] }
        set { self[NavigationUIThemeKey.self] = newValue }
    }
}

public extension View {
    /// Apply a theme to the Navigation UI view stack below.
    ///
    /// - Parameter theme: The ferrostar theme to apply.
    /// - Returns: the modified view.
    func navigationUITheme(_ theme: any NavigationUITheme) -> some View {
        environment(\.navigationUITheme, theme)
    }
}
