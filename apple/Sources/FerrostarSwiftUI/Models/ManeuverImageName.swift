import FerrostarCoreFFI

/// A maneuver image name def
public struct ManeuverImageName {
    /// The image name.
    public let value: String

    public init(maneuverType: ManeuverType, maneuverModifier: ManeuverModifier?) {
        value = [
            maneuverType.stringValue.replacingOccurrences(of: " ", with: "_"),
            maneuverModifier?.stringValue.replacingOccurrences(of: " ", with: "_"),
        ]
        .compactMap { $0 }
        .joined(separator: "_")
    }
}
