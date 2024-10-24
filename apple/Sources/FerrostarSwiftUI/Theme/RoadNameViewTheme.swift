import SwiftUI

public protocol RoadNameViewTheme: Equatable {
    /// The color for the road name label.
    var textColor: Color { get }

    /// The font for the road name label.
    var textFont: Font { get }

    /// The color of the background.
    var backgroundColor: Color { get }

    /// The color of the border around the view.
    var borderColor: Color { get }
}

public struct DefaultRoadNameViewTheme: RoadNameViewTheme {
    public var textColor: Color = .init(.white)
    public var textFont: Font = .callout.bold()
    public var backgroundColor: Color = .init(.systemBlue)
    public var borderColor: Color = .white

    public init() {}
}
