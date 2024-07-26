import SwiftUI

public protocol CustomizableNavigatingInnerGridView where Self: View {
    var topCenter: (() -> AnyView)? { get set }
    var topTrailing: (() -> AnyView)? { get set }
    var midLeading: (() -> AnyView)? { get set }
    var bottomTrailing: (() -> AnyView)? { get set }
}

public extension CustomizableNavigatingInnerGridView {
    
    /// Customize views on the navigating inner grid view that are not already being used.
    ///
    /// - Parameters:
    ///   - topCenter: The top center view content.
    ///   - topTrailing: The top trailing view content.
    ///   - midLeading: The mid leading view content.
    ///   - bottomTrailing: The bottom trailing view content.
    /// - Returns: The modified view.
    func innerGrid(
        @ViewBuilder topCenter: @escaping () -> some View = { Spacer() },
        @ViewBuilder topTrailing: @escaping () -> some View = { Spacer() },
        @ViewBuilder midLeading: @escaping () -> some View = { Spacer() },
        @ViewBuilder bottomTrailing: @escaping () -> some View = { Spacer() }
    ) -> some View {
        var newSelf = self
        newSelf.topCenter = { AnyView(topCenter()) }
        newSelf.topTrailing = { AnyView(topTrailing()) }
        newSelf.midLeading = { AnyView(midLeading()) }
        newSelf.bottomTrailing = { AnyView(bottomTrailing()) }
        return newSelf
    }
}
