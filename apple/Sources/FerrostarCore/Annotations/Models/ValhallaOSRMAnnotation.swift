import Foundation

/// A Valhalla OSRM flavored annotations object.
public struct ValhallaOSRMAnnotation: Codable, Equatable, Hashable {
    enum CodingKeys: String, CodingKey {
        case speedLimit = "maxspeed"
        case speed
        case distance
        case duration
    }

    /// The speed limit for the current line segment.
    public let speedLimit: MaxSpeed?

    /// The recommended travel speed in meters per second.
    public let speed: Double?

    /// The distance in meters of the geometry line segment.
    public let distance: Double?

    /// The duration in seconds of the geometry line segment.
    public let duration: Double?
}

public extension AnnotationPublisher {
    /// Create a Valhalla OSRM flavored annotation publisher
    ///
    /// - Parameter onError: An optional error closure (runs when a `DecoderError` occurs)
    /// - Returns: The annotation publisher.
    static func valhallaOSRM(
        onError: @escaping (Error) -> Void = { _ in }
    ) -> AnnotationPublisher<ValhallaOSRMAnnotation> {
        AnnotationPublisher<ValhallaOSRMAnnotation>(
            mapSpeedLimit: {
                $0?.speedLimit?.measurementValue
            },
            onError: onError
        )
    }
}
