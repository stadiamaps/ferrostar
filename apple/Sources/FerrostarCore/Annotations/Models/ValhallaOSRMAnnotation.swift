import Foundation

/// A Valhalla extended OSRM annotation object.
///
/// Describes attributes about a segment of an edge between two points
/// in a route step.
public struct ValhallaExtendedOSRMAnnotation: Codable, Equatable, Hashable {
    enum CodingKeys: String, CodingKey {
        case speedLimit = "maxspeed"
        case speed
        case distance
        case duration
    }

    /// The speed limit of the segment.
    public let speedLimit: MaxSpeed?

    /// The estimated speed of travel for the segment, in meters per second.
    public let speed: Double?

    /// The distance in meters of the segment.
    public let distance: Double?

    /// The estimated time to traverse the segment, in seconds.
    public let duration: Double?
}

public extension AnnotationPublisher {
    /// Create a Valhalla extended OSRM annotation publisher
    ///
    /// - Parameter onError: An optional error closure (runs when a `DecoderError` occurs)
    /// - Returns: The annotation publisher.
    static func valhallaExtendedOSRM(
        onError: @escaping (Error) -> Void = { _ in }
    ) -> AnnotationPublisher<ValhallaExtendedOSRMAnnotation> {
        AnnotationPublisher<ValhallaExtendedOSRMAnnotation>(
            mapSpeedLimit: {
                $0?.speedLimit?.measurementValue
            },
            onError: onError
        )
    }
}
