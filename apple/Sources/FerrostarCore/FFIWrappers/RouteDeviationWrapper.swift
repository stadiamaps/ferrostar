import CoreLocation
import FerrostarCoreFFI
import Foundation

private final class DetectorImpl: RouteDeviationDetector {
    let detectorFunc: @Sendable (Route, TripState) -> RouteDeviation

    init(detectorFunc: @escaping @Sendable (Route, TripState) -> RouteDeviation) {
        self.detectorFunc = detectorFunc
    }

    func checkRouteDeviation(route: Route, tripState: TripState) -> RouteDeviation {
        detectorFunc(route, tripState)
    }
}

/// A Swift wrapper around `UniFFI.RouteDeviationTracking`
public enum SwiftRouteDeviationTracking {
    case none

    /// Static threshold deviation tracking with hysteresis support.
    ///
    /// - Parameters:
    ///   - minimumHorizontalAccuracy: Minimum GPS accuracy required to trigger deviation checks
    ///   - maxAcceptableDeviation: Maximum distance from route before going off-route (must be >= 0)
    ///   - returnBuffer: Buffer distance for hysteresis (must be >= 0 and <= maxAcceptableDeviation).
    ///     If nil, defaults to 0 (no hysteresis).
    case staticThreshold(
        minimumHorizontalAccuracy: UInt16,
        maxAcceptableDeviation: Double,
        returnBuffer: Double? = nil
    )

    case custom(detector: @Sendable (Route, TripState) -> RouteDeviation)

    var ffiValue: FerrostarCoreFFI.RouteDeviationTracking {
        switch self {
        case .none:
            .none
        case let .staticThreshold(
            minimumHorizontalAccuracy: minimumHorizontalAccuracy,
            maxAcceptableDeviation: maxAcceptableDeviation,
            returnBuffer: returnBuffer
        ):
            .staticThreshold(
                StaticThresholdConfig(
                    minimumHorizontalAccuracy: minimumHorizontalAccuracy,
                    maxAcceptableDeviation: maxAcceptableDeviation,
                    returnBuffer: returnBuffer ?? 0.0
                )
            )
        case let .custom(detector: detectorFunc):
            .custom(detector: DetectorImpl(detectorFunc: detectorFunc))
        }
    }
}
