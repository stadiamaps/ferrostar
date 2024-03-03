import Foundation
import CoreLocation
import UniFFI

/// An observable state object, to make binding easier for SwiftUI applications.
///
/// While the core generally does not include UI, this is purely at the model layer and should be implemented
/// the same for all frontends.
public struct NavigationState: Hashable {
    public internal(set) var snappedLocation: UserLocation
    public internal(set) var heading: Heading?
    public internal(set) var courseOverGround: CourseOverGround?
    public internal(set) var fullRouteShape: [GeographicCoordinate]
    public internal(set) var currentStep: UniFFI.RouteStep?
    public internal(set) var visualInstructions: UniFFI.VisualInstruction?
    public internal(set) var spokenInstruction: UniFFI.SpokenInstruction?
    public internal(set) var distanceToNextManeuver: CLLocationDistance?
    /// Indicates when the core is calculating a new route due to the user being off route
    public internal(set) var isCalculatingNewRoute: Bool = false

    init(snappedLocation: CLLocation, heading: CLHeading? = nil, fullRoute: [CLLocationCoordinate2D], steps: [RouteStep]) {
        self.snappedLocation = UserLocation(clLocation: snappedLocation)! // TODO: Handle an error here if UserLocation is invalid?
        if let heading {
            self.heading = Heading(clHeading: heading)
        }
        self.courseOverGround = self.snappedLocation.courseOverGround
        self.fullRouteShape = fullRoute.map { GeographicCoordinate(cl: $0) }
        self.currentStep = steps.first!
    }
}
