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

    // TODO: Delete once using tripState
//    public internal(set) var snappedLocation: UserLocation
//    public internal(set) var routeDeviation: RouteDeviation?
//    public internal(set) var spokenInstruction: SpokenInstruction?
//    public internal(set) var visualInstruction: VisualInstruction?
//    public internal(set) var progress: TripProgress?

    // TODO: This may be part of TripState?
//    public internal(set) var heading: Heading?
//    public internal(set) var currentStep: RouteStep?

    // TODO: Delete once using routeGeometry
//    public internal(set) var fullRouteShape: [GeographicCoordinate]

//    init(
//        snappedLocation: UserLocation,
//        heading: Heading? = nil,
//        fullRouteShape: [GeographicCoordinate],
//        steps: [RouteStep],
//        progress: TripProgress? = nil
//    ) {
//        self.snappedLocation = snappedLocation
//        self.heading = heading
//        self.fullRouteShape = fullRouteShape
//        self.progress = progress
//        currentStep = steps.first
//        visualInstruction = currentStep?.visualInstructions.first
//        spokenInstruction = currentStep?.spokenInstructions.first
//    }
}
