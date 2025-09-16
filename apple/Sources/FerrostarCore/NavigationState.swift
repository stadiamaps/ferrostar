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

    init(navState: NavState, routeGeometry: [GeographicCoordinate], isCalculatingNewRoute: Bool = false) {
        tripState = navState.tripState
        self.routeGeometry = routeGeometry
        self.isCalculatingNewRoute = isCalculatingNewRoute
    }

    /// The current progress stats of the trip and current step.
    public var currentProgress: TripProgress? {
        guard case let .navigating(_, _, _, _, _, progress, _, _, _,
                                   _, _) = tripState
        else {
            return nil
        }

        return progress
    }

    /// An aggregated summary of the trip so far.
    public var currentSummary: TripSummary? {
        switch tripState {
        case let .navigating(_, _, _, _, _, _, summary, _, _, _, _),
             let .complete(_, summary):
            summary
        case .idle:
            nil
        }
    }

    /// The steps remaining after the current step.
    ///
    /// These are steps from the route that have not yet been travelled.
    public var remainingSteps: [RouteStep]? {
        guard case let .navigating(_, _, _, remainingSteps, _, _, _, _, _, _, _) = tripState else {
            return nil
        }

        return remainingSteps
    }

    /// The remaining waypoints on the navigation trip.
    public var remainingWaypoints: [Waypoint]? {
        guard case let .navigating(_, _, _, _, remainingWaypoints, _, _, _, _, _, _) = tripState else {
            return nil
        }

        return remainingWaypoints
    }

    /// The current route step.
    public var currentStep: RouteStep? {
        remainingSteps?.first
    }

    /// The current visual instruction.
    public var currentVisualInstruction: VisualInstruction? {
        guard case let .navigating(_, _, _, _, _, _, _, _, visualInstruction, _, _) = tripState else {
            return nil
        }

        return visualInstruction
    }

    /// The current route deviation state.
    public var currentDeviation: RouteDeviation? {
        guard case let .navigating(_, _, _, _, _, _, _, routeDeviation, _, _, _) = tripState else {
            return nil
        }

        return routeDeviation
    }

    /// The current geometry segment's annotations in a JSON string.
    ///
    /// A segment is the line between two coordinates on the geometry.
    public var currentAnnotationJSON: String? {
        guard case let .navigating(_, _, _, _, _, _, _, _, _, _, annotationJson) = tripState else {
            return nil
        }

        return annotationJson
    }

    /// A convenience determing wheather trip state is navigating.
    ///
    /// This can also be accessed through the ``tripState`` enum.
    public var isNavigating: Bool {
        switch tripState {
        case .navigating:
            true
        case .complete, .idle:
            false
        }
    }

    /// The road name of the current step if one is specified.
    public var currentRoadName: String? {
        guard case let .navigating(_, _, _, remainingSteps, _, _, _, _, _, _, _) = tripState else {
            return nil
        }

        let roadName = remainingSteps.first?.roadName?.trimmingCharacters(in: .whitespacesAndNewlines)

        if roadName?.isEmpty == true {
            return nil
        } else {
            return roadName
        }
    }

    /// Get the UI's preferred representation of User's location from the trip state.
    ///
    /// This will return the users snapped if the user is navigating and on route. Otherwise it will return the user's
    /// raw location.
    public var preferredUserLocation: UserLocation? {
        switch tripState {
        case let .idle(userLocation):
            userLocation
        case let .complete(userLocation, _):
            userLocation
        case let .navigating(_, userLocation, snappedUserLocation, _, _, _, _, deviation, _, _, _):
            switch deviation {
            case .noDeviation:
                snappedUserLocation
            case .offRoute:
                userLocation
            }
        }
    }
}
