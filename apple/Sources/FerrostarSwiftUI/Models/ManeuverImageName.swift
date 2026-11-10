import FerrostarCoreFFI

/// A maneuver image name def
public struct ManeuverImageName {
    /// The asset name to load before applying `shouldMirror`.
    public let value: String

    /// Reflect the selected asset horizontally when this is `true`.
    public let shouldMirror: Bool

    public init(maneuverType: ManeuverType,
                maneuverModifier: ManeuverModifier?,
                drivingSide: DrivingSide = .right)
    {
        let isRoundabout = maneuverType == .roundabout || maneuverType == .rotary
        shouldMirror = drivingSide == .left && (isRoundabout || maneuverModifier == .uTurn)

        // Reflecting a directional roundabout asset also reflects its exit.
        // Select the opposite exit first to preserve the requested direction.
        let imageModifier = shouldMirror && isRoundabout
            ? maneuverModifier?.horizontallyReflected : maneuverModifier
        value = [
            maneuverType.stringValue.replacingOccurrences(of: " ", with: "_"),
            imageModifier?.stringValue.replacingOccurrences(of: " ", with: "_"),
        ]
        .compactMap { $0 }
        .joined(separator: "_")
    }
}

private extension ManeuverModifier {
    var horizontallyReflected: ManeuverModifier {
        switch self {
        case .sharpRight: .sharpLeft
        case .right: .left
        case .slightRight: .slightLeft
        case .slightLeft: .slightRight
        case .left: .right
        case .sharpLeft: .sharpRight
        case .straight, .uTurn: self
        }
    }
}
