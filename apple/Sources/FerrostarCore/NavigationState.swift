import CoreLocation
<<<<<<< HEAD
import Foundation
import UniFFI
=======
import FerrostarCoreFFI
import Foundation
>>>>>>> 746c43483e74319176f21e1fe96b78c038215c0b

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

<<<<<<< HEAD
    init(snappedLocation: CLLocation, heading: CLHeading? = nil, fullRoute: [CLLocationCoordinate2D], steps: [RouteStep]) {
        self.snappedLocation = UserLocation(clLocation: snappedLocation)
        if let heading {
            self.heading = Heading(clHeading: heading)
        }
        courseOverGround = self.snappedLocation.courseOverGround
        fullRouteShape = fullRoute.map { GeographicCoordinate(cl: $0) }
=======
    init(
        snappedLocation: UserLocation,
        heading: Heading? = nil,
        fullRouteShape: [GeographicCoordinate],
        steps: [RouteStep]
    ) {
        self.snappedLocation = snappedLocation
        self.heading = heading
        self.fullRouteShape = fullRouteShape
>>>>>>> 746c43483e74319176f21e1fe96b78c038215c0b
        currentStep = steps.first!
    }
}
