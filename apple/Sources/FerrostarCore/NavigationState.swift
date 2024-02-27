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

    init(snappedLocation: CLLocation, heading: CLHeading? = nil, fullRoute: [CLLocationCoordinate2D], steps: [RouteStep]) {
        self.snappedLocation = UserLocation(clLocation: snappedLocation)
        if let heading {
            self.heading = Heading(clHeading: heading)
        }
        self.courseOverGround = CourseOverGround(degrees: snappedLocation.course,
                                                 accuracy: snappedLocation.courseAccuracy)
        self.fullRouteShape = fullRoute.map { GeographicCoordinate(lat: $0.latitude, lng: $0.longitude) }
        self.currentStep = steps.first!
    }

    public static let pedestrianExample = NavigationState(
        snappedLocation: CLLocation(latitude: samplePedestrianWaypoints.first!.latitude,
                                    longitude: samplePedestrianWaypoints.first!.longitude),
        fullRoute: samplePedestrianWaypoints,
        steps: []
    )

    public static func modifiedPedestrianExample(droppingNWaypoints n: Int) -> NavigationState {
        let remainingLocations = Array(samplePedestrianWaypoints.dropFirst(n))
        let lastUserLocation = remainingLocations.first!

        var result = NavigationState(
            snappedLocation: CLLocation(latitude: samplePedestrianWaypoints.first!.latitude,
                                        longitude: samplePedestrianWaypoints.first!.longitude),
            fullRoute: samplePedestrianWaypoints,
            steps: [UniFFI.RouteStep(
                geometry: [lastUserLocation.geographicCoordinates],
                distance: 100, roadName: "Jefferson St.",
                instruction: "Walk west on Jefferson St.",
                visualInstructions: [
                    UniFFI.VisualInstruction(
                        primaryContent: VisualInstructionContent(text: "Hyde Street", maneuverType: .turn, maneuverModifier: .left, roundaboutExitDegrees: nil),
                        secondaryContent: nil, triggerDistanceBeforeManeuver: 42.0)
                ],
                spokenInstructions: [])
            ])

        result.snappedLocation = UserLocation(
            coordinates: GeographicCoordinate(lat: samplePedestrianWaypoints.first!.latitude,
                                              lng: samplePedestrianWaypoints.first!.longitude),
            horizontalAccuracy: 10,
            courseOverGround: CourseOverGround(degrees: 0, accuracy: 10),
            timestamp: Date()
        )

        return result
    }
}

// Derived from the Stadia Maps map matching example
private let samplePedestrianWaypoints = [
    CLLocationCoordinate2D(latitude: 37.807770999999995, longitude: -122.41970699999999),
    CLLocationCoordinate2D(latitude: 37.807680999999995, longitude: -122.42041599999999),
    CLLocationCoordinate2D(latitude: 37.807623, longitude: -122.42040399999999),
    CLLocationCoordinate2D(latitude: 37.807587, longitude: -122.420678),
    CLLocationCoordinate2D(latitude: 37.807527, longitude: -122.420666),
    CLLocationCoordinate2D(latitude: 37.807514, longitude: -122.420766),
    CLLocationCoordinate2D(latitude: 37.807475, longitude: -122.420757),
    CLLocationCoordinate2D(latitude: 37.807438, longitude: -122.42073599999999),
    CLLocationCoordinate2D(latitude: 37.807403, longitude: -122.420721),
    CLLocationCoordinate2D(latitude: 37.806951999999995, longitude: -122.420633),
    CLLocationCoordinate2D(latitude: 37.806779999999996, longitude: -122.4206),
    CLLocationCoordinate2D(latitude: 37.806806, longitude: -122.42069599999999),
    CLLocationCoordinate2D(latitude: 37.806781, longitude: -122.42071999999999),
    CLLocationCoordinate2D(latitude: 37.806754999999995, longitude: -122.420746),
    CLLocationCoordinate2D(latitude: 37.806739, longitude: -122.420761),
    CLLocationCoordinate2D(latitude: 37.806701, longitude: -122.42105699999999),
    CLLocationCoordinate2D(latitude: 37.806616999999996, longitude: -122.42171599999999),
    CLLocationCoordinate2D(latitude: 37.806562, longitude: -122.42214299999999),
    CLLocationCoordinate2D(latitude: 37.806464999999996, longitude: -122.422123),
    CLLocationCoordinate2D(latitude: 37.806453, longitude: -122.42221699999999),
    CLLocationCoordinate2D(latitude: 37.806439999999995, longitude: -122.42231),
    CLLocationCoordinate2D(latitude: 37.806394999999995, longitude: -122.422585),
    CLLocationCoordinate2D(latitude: 37.806305, longitude: -122.423289),
    CLLocationCoordinate2D(latitude: 37.806242999999995, longitude: -122.423773),
    CLLocationCoordinate2D(latitude: 37.806232, longitude: -122.423862),
    CLLocationCoordinate2D(latitude: 37.806152999999995, longitude: -122.423846),
    CLLocationCoordinate2D(latitude: 37.805687999999996, longitude: -122.423755),
    CLLocationCoordinate2D(latitude: 37.805385, longitude: -122.42369),
    CLLocationCoordinate2D(latitude: 37.805371, longitude: -122.423797),
    CLLocationCoordinate2D(latitude: 37.805306, longitude: -122.42426999999999),
    CLLocationCoordinate2D(latitude: 37.805259, longitude: -122.42463699999999),
    CLLocationCoordinate2D(latitude: 37.805192, longitude: -122.425147),
    CLLocationCoordinate2D(latitude: 37.805184, longitude: -122.42521199999999),
    CLLocationCoordinate2D(latitude: 37.805096999999996, longitude: -122.425218),
    CLLocationCoordinate2D(latitude: 37.805074999999995, longitude: -122.42539699999999),
    CLLocationCoordinate2D(latitude: 37.804992, longitude: -122.425373),
    CLLocationCoordinate2D(latitude: 37.804852, longitude: -122.425345),
    CLLocationCoordinate2D(latitude: 37.804657, longitude: -122.42530599999999),
    CLLocationCoordinate2D(latitude: 37.804259, longitude: -122.425224),
    CLLocationCoordinate2D(latitude: 37.804249, longitude: -122.425339),
    CLLocationCoordinate2D(latitude: 37.804128, longitude: -122.425314),
    CLLocationCoordinate2D(latitude: 37.804109, longitude: -122.425461),
    CLLocationCoordinate2D(latitude: 37.803956, longitude: -122.426678),
    CLLocationCoordinate2D(latitude: 37.803944, longitude: -122.42677599999999),
    CLLocationCoordinate2D(latitude: 37.803931, longitude: -122.42687699999999),
    CLLocationCoordinate2D(latitude: 37.803736, longitude: -122.42841899999999),
    CLLocationCoordinate2D(latitude: 37.803695, longitude: -122.428411),
]
