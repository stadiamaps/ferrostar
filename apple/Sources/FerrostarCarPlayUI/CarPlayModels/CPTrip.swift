import CarPlay
import FerrostarCoreFFI
import FerrostarSwiftUI

private let RouteKey = "com.stadiamaps.ferrostar.route"

extension CPRouteChoice {
    convenience init(
        summaryVariants: [String],
        additionalInformationVariants: [String],
        selectionSummaryVariants: [String],
        route: Route
    ) {
        self.init(
            summaryVariants: summaryVariants,
            additionalInformationVariants: additionalInformationVariants,
            selectionSummaryVariants: selectionSummaryVariants
        )
        userInfo = [RouteKey: route]
    }

    public var route: Route? {
        guard let info = userInfo as? [String: Any] else { return nil }
        return info[RouteKey] as? Route
    }
}

private extension [Waypoint] {
    func originDestination() throws -> (origin: MKMapItem, destination: MKMapItem) {
        guard let originWaypoint = first, let destinationWaypoint = last else {
            throw FerrostarCarPlayError.invalidTrip
        }

        // TODO: We could improve this with an address dictionary/CPPostalAddress if we enhanced
        //       the optional metadata associated to a ferrostar waypoint. Especially common for
        //       the origin and destination, less so for intermediate.
        let origin = MKMapItem(placemark: MKPlacemark(coordinate: originWaypoint.coordinate.clLocationCoordinate2D))
        let destination =
            MKMapItem(placemark: MKPlacemark(coordinate: destinationWaypoint.coordinate.clLocationCoordinate2D))
        return (origin, destination)
    }
}

public extension CPTrip {
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
        distanceFormatter: Formatter,
        durationFormatter: DateComponentsFormatter
    ) throws -> CPTrip {
        guard let route = routes.first else {
            throw FerrostarCarPlayError.noRoutes
        }
        let (origin, destination) = try route.waypoints.originDestination()

        return fromFerrostar(
            routes: routes,
            origin: origin,
            destination: destination,
            distanceFormatter: distanceFormatter,
            durationFormatter: durationFormatter
        )
    }

    static func fromFerrostar(
        routes: [Route],
        origin: MKMapItem,
        destination: MKMapItem,
        distanceFormatter: Formatter,
        durationFormatter _: DateComponentsFormatter
    ) -> CPTrip {
        let routeChoices: [CPRouteChoice] = routes.enumerated().map { index, route in
            let routeNumber = index + 1
            let distance = distanceFormatter.string(for: route.distance)
            let summary = [distance].compactMap { $0 }.joined(separator: ", ")

            return CPRouteChoice(
                summaryVariants: ["Route \(routeNumber)"],
                additionalInformationVariants: [summary],
                selectionSummaryVariants: ["Selected Route \(routeNumber)"],
                route: route
            )
        }

        return CPTrip(
            origin: origin,
            destination: destination,
            routeChoices: routeChoices
        )
    }
}
