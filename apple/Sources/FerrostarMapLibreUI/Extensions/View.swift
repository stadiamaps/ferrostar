import SwiftUI

extension View {
    /// Given the view's `geometry`, synchronizes `childInsets`, such that
    /// accumulating the child's insets with the parents insets, will be at least `minimumInset`.
    ///
    /// ```
    ///    Given a minimumInset of 16:
    ///    +-------------------------------------------------------------+
    ///    |                       `parentGeometry`                      |
    ///    |   +-----------------------------------------------------+   |
    ///    |   |     `parentGeometry.safeAreaInsets` (Top: 16)       |   |
    ///    |   |   +---------------------------------------------+   |   |
    ///    |   |   |     insets added by this method (Top: 0)    |   |   |
    ///    |   |   |   +------------------------------------+    |   |   |
    ///    |   |   |   |                                    |    |   |   |
    ///    |   | 8 | 8 |        child view (self)           | 16 | 0 |   |
    ///    |   |   |   |                                    |    |   |   |
    ///    |   |   |   +------------------------------------+    |   |   |
    ///    |   |   |    insets added by this method (Bottom: 0)  |   |   |
    ///    |   |   +---------------------------------------------+   |   |
    ///    |   |    `parentGeometry.safeAreaInsets` (Bottom: 20)     |   |
    ///    |   +-----------------------------------------------------+   |
    ///    |                                                             |
    ///    +-------------------------------------------------------------+
    /// ```
    func complementSafeAreaInsets(
        parentGeometry: GeometryProxy,
        minimumInset: EdgeInsets = EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16)
    ) -> some View {
        ComplementingSafeAreaView(content: self, parentGeometry: parentGeometry, minimumInset: minimumInset)
    }

    /// Do something reasonable-ish for clients that don't yet support
    /// safeAreaPadding - in this case, fall back to regular padding.
    func safeAreaPaddingPolyfill(_ insets: EdgeInsets) -> AnyView {
        if #available(iOS 17.0, *) {
            AnyView(self.safeAreaPadding(insets))
        } else {
            AnyView(padding(insets))
        }
    }
}

struct ComplementingSafeAreaView<V: View>: View {
    var content: V

    var parentGeometry: GeometryProxy
    var minimumInset: EdgeInsets

    @State
    var childInsets: EdgeInsets = .init()

    static func complement(parentInset: EdgeInsets, minimumInset: EdgeInsets) -> EdgeInsets {
        var innerInsets = parentInset
        innerInsets.top = max(0, minimumInset.top - parentInset.top)
        innerInsets.bottom = max(0, minimumInset.bottom - parentInset.bottom)
        innerInsets.leading = max(0, minimumInset.leading - parentInset.leading)
        innerInsets.trailing = max(0, minimumInset.trailing - parentInset.trailing)
        return innerInsets
    }

    var body: some View {
        content.onAppear {
            childInsets = ComplementingSafeAreaView.complement(
                parentInset: parentGeometry.safeAreaInsets,
                minimumInset: minimumInset
            )
        }.onChange(of: parentGeometry.safeAreaInsets) { newValue in
            childInsets = ComplementingSafeAreaView.complement(parentInset: newValue, minimumInset: minimumInset)
        }.safeAreaPaddingPolyfill(childInsets)
    }
}
