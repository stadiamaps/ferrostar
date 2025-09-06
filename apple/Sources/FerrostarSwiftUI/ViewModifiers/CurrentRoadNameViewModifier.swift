import FerrostarCore
import SwiftUI

/// An extension for a NavigationView that can host a Current Road View.
public protocol CurrentRoadNameViewHost where Self: View {
    var currentRoadNameView: ((NavigationState?) -> AnyView)? { get set }

    /// Override the Current Road Name View with a custom view.
    ///
    /// - Parameter currentRoadNameView: The custom current road name view to display.
    /// - Returns: The modified view.
    func navigationViewCurrentRoadView(
        @ViewBuilder _ currentRoadNameView: @escaping (NavigationState?) -> some View
    ) -> Self

    @available(*, deprecated, renamed: "navigationViewCurrentRoadView")
    func navigationCurrentRoadView(
        @ViewBuilder currentRoadNameViewBuilder: @escaping () -> some View
    ) -> Self
}

public extension CurrentRoadNameViewHost {
    func navigationViewCurrentRoadView(
        @ViewBuilder _ currentRoadNameView: @escaping (NavigationState?) -> some View
    ) -> Self {
        var mutableSelf = self
        mutableSelf.currentRoadNameView = { AnyView(currentRoadNameView($0)) }
        return mutableSelf
    }

    @available(*, deprecated, renamed: "navigationViewCurrentRoadView")
    func navigationCurrentRoadView(@ViewBuilder currentRoadNameViewBuilder: @escaping () -> some View) -> Self {
        navigationViewCurrentRoadView { _ in
            currentRoadNameViewBuilder()
        }
    }
}
