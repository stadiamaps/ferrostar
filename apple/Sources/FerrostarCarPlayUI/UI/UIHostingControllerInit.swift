import SwiftUI
import UIKit

public extension UIHostingController where Content: View {
    /// Create a SwiftUI view in a UIHosting controller with a closure init.
    ///
    /// - Parameter content: The convent view builder.
    convenience init(@ViewBuilder content: () -> Content) {
        self.init(rootView: content())
    }
}
