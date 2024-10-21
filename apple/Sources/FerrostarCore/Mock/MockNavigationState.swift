import CoreLocation
import FerrostarCoreFFI
import Foundation

public extension NavigationState {
    static let pedestrianExample = NavigationState(
        tripState: .navigating(
            currentStepGeometryIndex: 0,
            snappedUserLocation: UserLocation(
                latitude: samplePedestrianWaypoints.first!.lat,
                longitude: samplePedestrianWaypoints.first!.lng,
                horizontalAccuracy: 0,
                course: 0,
                courseAccuracy: 0,
                timestamp: Date(),
                speed: 0,
                speedAccuracy: 0
            ),
            remainingSteps: [],
            remainingWaypoints: [],
            progress: TripProgress(
                distanceToNextManeuver: 0,
                distanceRemaining: 0,
                durationRemaining: 0
            ),
            deviation: .noDeviation,
            visualInstruction: nil,
            spokenInstruction: nil,
            annotationJson: nil
        ),
        routeGeometry: samplePedestrianWaypoints,
        isCalculatingNewRoute: false
    )

    static func modifiedPedestrianExample(droppingNWaypoints n: Int) -> NavigationState {
        let remainingLocations = Array(samplePedestrianWaypoints.dropFirst(n))
        let lastUserLocation = remainingLocations.first!

        return NavigationState(
            tripState: .navigating(
                currentStepGeometryIndex: 0,
                snappedUserLocation: UserLocation(
                    coordinates: samplePedestrianWaypoints.first!,
                    horizontalAccuracy: 10,
                    courseOverGround: CourseOverGround(degrees: 0, accuracy: 10),
                    timestamp: Date(),
                    speed: Speed(value: 0, accuracy: 2)
                ),
                remainingSteps: [
                    RouteStep(
                        geometry: [lastUserLocation],
                        distance: 100,
                        duration: 99,
                        roadName: "Jefferson St.",
                        instruction: "Walk west on Jefferson St.",
                        visualInstructions: [
                            VisualInstruction(
                                primaryContent: VisualInstructionContent(
                                    text: "Hyde Street",
                                    maneuverType: .turn,
                                    maneuverModifier: .left,
                                    roundaboutExitDegrees: nil,
                                    laneInfo: nil
                                ),
                                secondaryContent: nil,
                                subContent: nil,
                                triggerDistanceBeforeManeuver: 42.0
                            ),
                        ],
                        spokenInstructions: [],
                        annotations: nil
                    ),
                ],
                remainingWaypoints: [],
                progress: TripProgress(
                    distanceToNextManeuver: 5,
                    distanceRemaining: 100,
                    durationRemaining: 99
                ),
                deviation: .noDeviation,
                visualInstruction: nil,
                spokenInstruction: nil,
                annotationJson: nil
            ),
            routeGeometry: samplePedestrianWaypoints,
            isCalculatingNewRoute: false
        )
    }
}

// Derived from the Stadia Maps map matching example
private let samplePedestrianWaypoints = [
    GeographicCoordinate(lat: 37.807770999999995, lng: -122.41970699999999),
    GeographicCoordinate(lat: 37.807680999999995, lng: -122.42041599999999),
    GeographicCoordinate(lat: 37.807623, lng: -122.42040399999999),
    GeographicCoordinate(lat: 37.807587, lng: -122.420678),
    GeographicCoordinate(lat: 37.807527, lng: -122.420666),
    GeographicCoordinate(lat: 37.807514, lng: -122.420766),
    GeographicCoordinate(lat: 37.807475, lng: -122.420757),
    GeographicCoordinate(lat: 37.807438, lng: -122.42073599999999),
    GeographicCoordinate(lat: 37.807403, lng: -122.420721),
    GeographicCoordinate(lat: 37.806951999999995, lng: -122.420633),
    GeographicCoordinate(lat: 37.806779999999996, lng: -122.4206),
    GeographicCoordinate(lat: 37.806806, lng: -122.42069599999999),
    GeographicCoordinate(lat: 37.806781, lng: -122.42071999999999),
    GeographicCoordinate(lat: 37.806754999999995, lng: -122.420746),
    GeographicCoordinate(lat: 37.806739, lng: -122.420761),
    GeographicCoordinate(lat: 37.806701, lng: -122.42105699999999),
    GeographicCoordinate(lat: 37.806616999999996, lng: -122.42171599999999),
    GeographicCoordinate(lat: 37.806562, lng: -122.42214299999999),
    GeographicCoordinate(lat: 37.806464999999996, lng: -122.422123),
    GeographicCoordinate(lat: 37.806453, lng: -122.42221699999999),
    GeographicCoordinate(lat: 37.806439999999995, lng: -122.42231),
    GeographicCoordinate(lat: 37.806394999999995, lng: -122.422585),
    GeographicCoordinate(lat: 37.806305, lng: -122.423289),
    GeographicCoordinate(lat: 37.806242999999995, lng: -122.423773),
    GeographicCoordinate(lat: 37.806232, lng: -122.423862),
    GeographicCoordinate(lat: 37.806152999999995, lng: -122.423846),
    GeographicCoordinate(lat: 37.805687999999996, lng: -122.423755),
    GeographicCoordinate(lat: 37.805385, lng: -122.42369),
    GeographicCoordinate(lat: 37.805371, lng: -122.423797),
    GeographicCoordinate(lat: 37.805306, lng: -122.42426999999999),
    GeographicCoordinate(lat: 37.805259, lng: -122.42463699999999),
    GeographicCoordinate(lat: 37.805192, lng: -122.425147),
    GeographicCoordinate(lat: 37.805184, lng: -122.42521199999999),
    GeographicCoordinate(lat: 37.805096999999996, lng: -122.425218),
    GeographicCoordinate(lat: 37.805074999999995, lng: -122.42539699999999),
    GeographicCoordinate(lat: 37.804992, lng: -122.425373),
    GeographicCoordinate(lat: 37.804852, lng: -122.425345),
    GeographicCoordinate(lat: 37.804657, lng: -122.42530599999999),
    GeographicCoordinate(lat: 37.804259, lng: -122.425224),
    GeographicCoordinate(lat: 37.804249, lng: -122.425339),
    GeographicCoordinate(lat: 37.804128, lng: -122.425314),
    GeographicCoordinate(lat: 37.804109, lng: -122.425461),
    GeographicCoordinate(lat: 37.803956, lng: -122.426678),
    GeographicCoordinate(lat: 37.803944, lng: -122.42677599999999),
    GeographicCoordinate(lat: 37.803931, lng: -122.42687699999999),
    GeographicCoordinate(lat: 37.803736, lng: -122.42841899999999),
    GeographicCoordinate(lat: 37.803695, lng: -122.428411),
]
