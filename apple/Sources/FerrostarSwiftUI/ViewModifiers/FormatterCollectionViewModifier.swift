import SwiftUI

struct FormatterCollectionKey: EnvironmentKey {
    static var defaultValue: any FormatterCollection = FoundationFormatterCollection()
}

public extension EnvironmentValues {
    var navigationFormatterCollection: any FormatterCollection {
        get { self[FormatterCollectionKey.self] }
        set { self[FormatterCollectionKey.self] = newValue }
    }
}

public extension View {
    /// Apply the ferrostar formatters to the view stack below.
    ///
    /// This controls all value formatters for subviews in the ferrostar navigation view stack.
    ///
    /// - Parameter formatterCollection: The formatter collection for values in the UI.
    /// - Returns: the modified view.
    func navigationFormatterCollection(_ formatterCollection: any FormatterCollection) -> some View {
        environment(\.navigationFormatterCollection, formatterCollection)
    }
}
