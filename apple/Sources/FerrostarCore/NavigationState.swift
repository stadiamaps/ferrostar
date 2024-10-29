import CoreLocation
import FerrostarCoreFFI
import Foundation

/// An observable state object, to make binding easier for SwiftUI applications.
///
/// While the core generally does not include UI, this is purely at the model layer and should be implemented
/// the same for all frontends.
public struct NavigationState: Hashable {
    public internal(set) var tripState: TripState
    public internal(set) var routeGeometry: [GeographicCoordinate]

    // TODO: This probably gets removed once we have an observer protocol

    /// Indicates when the core is calculating a new route due to the user being off route
    public internal(set) var isCalculatingNewRoute: Bool = false

    init(tripState: TripState, routeGeometry: [GeographicCoordinate], isCalculatingNewRoute: Bool = false) {
        self.tripState = tripState
        self.routeGeometry = routeGeometry
        self.isCalculatingNewRoute = isCalculatingNewRoute
    }

    public var currentProgress: TripProgress? {
        guard case let .navigating(_, _, _, _, progress, _, _,
                                   _, _) = tripState
        else {
            return nil
        }

        return progress
    }

    public var currentVisualInstruction: VisualInstruction? {
        guard case let .navigating(_, _, _, _, _, _, visualInstruction, _, _) = tripState else {
            return nil
        }

        return visualInstruction
    }

    public var remainingSteps: [RouteStep]? {
        guard case let .navigating(_, _, remainingSteps, _, _, _, _, _, _) = tripState else {
            return nil
        }

        return remainingSteps
    }

    /// The current geometry segment's annotations in a JSON string.
    ///
    /// A segment is the line between two coordinates on the geometry.
    public var currentAnnotationJSON: String? {
        guard case let .navigating(_, _, _, _, _, _, _, _, annotationJson) = tripState else {
            return nil
        }

        return annotationJson
    }

    public var isNavigating: Bool {
        switch tripState {
        case .navigating:
            true
        case .complete, .idle:
            false
        }
    }

    public var currentRoadName: String? {
        guard case let .navigating(_, _, remainingSteps, _, _, _, _, _, _) = tripState else {
            return nil
        }

        let roadName = remainingSteps.first?.roadName?.trimmingCharacters(in: .whitespacesAndNewlines)

        if roadName?.isEmpty == true {
            return nil
        } else {
            return roadName
        }
    }
}
