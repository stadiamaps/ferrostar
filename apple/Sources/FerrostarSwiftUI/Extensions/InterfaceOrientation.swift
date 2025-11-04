import SwiftUI

public extension GeometryProxy {
    /// True if the rendered view is wider than it is high.
    ///
    /// We use this for making certain layout decisions that are not as accurately represented
    /// with other methods like size classes.
    ///
    /// NOTE: This is a public computed property,
    /// but you sholud not rely on this in your applications as we may ditch this
    /// if a better model presents itself.
    /// It is only public so that we can use it internally to other Ferrostar modules.
    ///
    /// See https://github.com/stadiamaps/ferrostar/issues/734
    /// for discussion.
    var isLandscape: Bool {
        size.width > size.height
    }
}
