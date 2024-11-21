import SwiftUI
import UIKit

extension UIHostingController where Content: View {
    /// Create a SwiftUI view in a UIHosting controller with a closure init.
    ///
    /// - Parameter content: The convent view builder.
    convenience init(@ViewBuilder content: () -> some View) {
        self.init(rootView: content())
    }
}
