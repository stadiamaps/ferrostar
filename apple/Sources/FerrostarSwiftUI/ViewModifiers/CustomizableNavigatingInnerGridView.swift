import SwiftUI

public protocol CustomizableNavigatingInnerGridView where Self: View {
    var topCenter: (() -> AnyView)? { get set }
    var topTrailing: (() -> AnyView)? { get set }
    var midLeading: (() -> AnyView)? { get set }
    var bottomTrailing: (() -> AnyView)? { get set }
}

public extension CustomizableNavigatingInnerGridView {
    
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
