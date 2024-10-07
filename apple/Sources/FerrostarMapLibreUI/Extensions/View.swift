import SwiftUI

extension View {
    /// Given the parent view's `geometry`, synchronizes this views `safeAreaInsets`, such that
    /// accumulating this view's insets with the parents insets, will be at least `minimumInset`.
    ///
    /// ```
    ///    Given minimumInsets of 16:
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
        minimumInsets: EdgeInsets = EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16)
    ) -> some View {
        ComplementingSafeAreaView(content: self, parentGeometry: parentGeometry, minimumInsets: minimumInsets)
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
    var minimumInsets: EdgeInsets

    @State
    var childInsets: EdgeInsets = .init()

    static func complement(parentInsets: EdgeInsets, minimumInsets: EdgeInsets) -> EdgeInsets {
        var innerInsets = parentInsets
        innerInsets.top = max(0, minimumInsets.top - parentInsets.top)
        innerInsets.bottom = max(0, minimumInsets.bottom - parentInsets.bottom)
        innerInsets.leading = max(0, minimumInsets.leading - parentInsets.leading)
        innerInsets.trailing = max(0, minimumInsets.trailing - parentInsets.trailing)
        return innerInsets
    }

    var body: some View {
        content.onAppear {
            childInsets = ComplementingSafeAreaView.complement(
                parentInsets: parentGeometry.safeAreaInsets,
                minimumInsets: minimumInsets
            )
        }.onChange(of: parentGeometry.safeAreaInsets) { newValue in
            childInsets = ComplementingSafeAreaView.complement(parentInsets: newValue, minimumInsets: minimumInsets)
        }.safeAreaPaddingPolyfill(childInsets)
    }
}
