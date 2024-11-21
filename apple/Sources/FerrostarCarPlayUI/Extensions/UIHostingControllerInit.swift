import UIKit
import SwiftUI

extension UIHostingController where Content: View {
    
    /// Create a SwiftUI view in a UIHosting controller with a closure init.
    ///
    /// - Parameter content: The convent view builder.
    convenience init<V: View>(@ViewBuilder content: () -> V) where V == Content {
        self.init(rootView: content())
    }
}
