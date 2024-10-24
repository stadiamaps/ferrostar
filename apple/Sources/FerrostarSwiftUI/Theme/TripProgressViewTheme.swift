import SwiftUI

public enum TripProgressViewStyle: Equatable {
    /// The simplified/default which only shows actual values
    case simplified

    /// An expanded informational progress view that labels each value.
    case informational
}

public protocol TripProgressViewTheme: Equatable {
    /// The style of the trip progress view controls the general appearance/level of detail.
    var style: TripProgressViewStyle { get }

    /// The color for the measurement values (top row)
    var measurementColor: Color { get }

    /// The font for the measurement values (top row)
    var measurementFont: Font { get }

    /// The color for the secondary text.
    var secondaryColor: Color { get }

    /// The font for the secondary text.
    var secondaryFont: Font { get }

    /// The color of the background.
    var backgroundColor: Color { get }
}

public struct DefaultTripProgressViewTheme: TripProgressViewTheme {
    public var style: TripProgressViewStyle = .simplified
    public var measurementColor: Color = .primary
    public var measurementFont: Font = .title2.bold()
    public var secondaryColor: Color = .secondary
    public var secondaryFont: Font = .subheadline
    public var backgroundColor: Color = .init(.systemBackground)

    public init() {}
}
