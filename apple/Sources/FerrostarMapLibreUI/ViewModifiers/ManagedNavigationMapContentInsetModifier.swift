import SwiftUI

struct ManagedNavigationMapContentInsetModifier: ViewModifier {
    let inset: NavigationMapViewContentInsetMode?

    @ViewBuilder
    func body(content: Content) -> some View {
        if let inset {
            content.mapContentInset(inset.uiEdgeInsets)
        } else {
            content
        }
    }
}
