import CoreLocation
import FerrostarCoreFFI
import Foundation

/// An observable state object, to make binding easier for SwiftUI applications.
///
/// While the core generally does not include UI, this is purely at the model layer and should be implemented
/// the same for all frontends.
public struct NavigationState: Hashable {
    public internal(set) var snappedLocation: UserLocation
    public internal(set) var heading: Heading?
    public internal(set) var fullRouteShape: [GeographicCoordinate]
    public internal(set) var currentStep: RouteStep?
    public internal(set) var visualInstructions: VisualInstruction?
    public internal(set) var spokenInstruction: SpokenInstruction?
    public internal(set) var distanceToNextManeuver: CLLocationDistance?
    /// Indicates when the core is calculating a new route due to the user being off route
    public internal(set) var isCalculatingNewRoute: Bool = false
    public internal(set) var routeDeviation: RouteDeviation?

    init(
        snappedLocation: UserLocation,
        heading: Heading? = nil,
        fullRouteShape: [GeographicCoordinate],
        steps: [RouteStep]
    ) {
        self.snappedLocation = snappedLocation
        self.heading = heading
        self.fullRouteShape = fullRouteShape
        currentStep = steps.first
        visualInstructions = currentStep?.visualInstructions.first
        spokenInstruction = currentStep?.spokenInstructions.first
        distanceToNextManeuver = currentStep?.distance
    }
}
