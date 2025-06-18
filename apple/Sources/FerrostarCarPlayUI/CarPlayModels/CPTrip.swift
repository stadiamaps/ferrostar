import CarPlay
import FerrostarCoreFFI
import FerrostarSwiftUI

private let RouteKey = "com.stadiamaps.ferrostar.route"

extension CPRouteChoice {
    private var userDictionary: [String: Any]? {
        get {
            userInfo as? [String: Any] ?? [:]
        }
        set {
            userInfo = newValue
        }
    }

    public var route: Route? {
        get {
            userDictionary?[RouteKey] as? Route
        }
        set {
            var info = userDictionary ?? [:]
            info[RouteKey] = newValue
            userDictionary = info
        }
    }
}

extension CPTrip {
    /// Create a new CarPlay Trip definition from the routes and waypoints supplied to/by
    /// Ferrostar. This object is used to outline the various route option metadata and the associated
    /// origin and destination to the user.
    ///
    /// - Parameters:
    ///   - routes: The route's loaded by ferrostar
    ///   - waypoints: The associated waypoints.
    ///   - distanceFormatter: Used to format the distance metadata associated with a route.
    ///   - durationFormatter: Used to format the duration metadata associated with a route.
    /// - Returns: The CarPlay trip that can be applied used for CPNavigationSession.
    static func fromFerrostar(
        routes: [Route],
        waypoints: [Waypoint],
        distanceFormatter: Formatter,
        durationFormatter _: DateComponentsFormatter
    ) throws -> CPTrip {
        guard let originWaypoint = waypoints.first,
              let destinationWaypoint = waypoints.last
        else {
            throw FerrostarCarPlayError.invalidTrip
        }

        // TODO: We could improve this with an address dictionary/CPPostalAddress if we enhanced
        //       the optional metadata associated to a ferrostar waypoint. Especially common for
        //       the origin and destination, less so for intermediate.
        let origin = MKPlacemark(coordinate: originWaypoint.coordinate.clLocationCoordinate2D)
        let destination = MKPlacemark(coordinate: destinationWaypoint.coordinate.clLocationCoordinate2D)

        let routeChoices: [CPRouteChoice] = routes.enumerated().map { index, route in
            let routeNumber = index + 1
            let distance = distanceFormatter.string(for: route.distance)
            let summary = [distance].compactMap { $0 }.joined(separator: ", ")

            let routeChoice = CPRouteChoice(
                summaryVariants: ["Route \(routeNumber)"],
                additionalInformationVariants: [summary],
                selectionSummaryVariants: ["Selected Route \(routeNumber)"]
            )
            routeChoice.route = route
            return routeChoice
        }

        return CPTrip(
            origin: .init(placemark: origin),
            destination: .init(placemark: destination),
            routeChoices: routeChoices
        )
    }
}
