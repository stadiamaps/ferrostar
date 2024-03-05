//
//  Navigation Delegate.swift
//  Ferrostar Demo
//
//  Created by Ian Wagner on 2024-02-25.
//

import CoreLocation
import FerrostarCore

/// Not all navigation apps will require a navigation delegate. In fact, we hope that most don't!
/// In case you do though, this sample implementation shows what you'll need to get started
/// by re-implementing the default behaviors of the core.
class NavigationDelegate: FerrostarCoreDelegate {
    func core(_: FerrostarCore, correctiveActionForDeviation _: Double, remainingWaypoints waypoints: [CLLocationCoordinate2D]) -> CorrectiveAction {
        // If the user is off course, we'll try to calculate a new route using the remaining waypoints.
        return .getNewRoutes(waypoints: waypoints)
    }

    func core(_ core: FerrostarCore, loadedAlternateRoutes routes: [Route]) {
        // Automatically accept the first new route if the framework was calculating a new route
        // due to the user being off course.
        // Pretty sensible default.
        if core.state?.isCalculatingNewRoute ?? false,
           let route = routes.first
        {
            do {
                // Most implementations will probably reuse existing configs (the default implementation does),
                // but we provide devs with flexibility here.
                let config = NavigationControllerConfig(stepAdvance: .relativeLineStringDistance(minimumHorizontalAccuracy: 32, automaticAdvanceDistance: 10), routeDeviationTracking: .staticThreshold(minimumHorizontalAccuracy: 25, maxAcceptableDeviation: 20))
                try core.startNavigation(
                    route: route,
                    config: config
                )
            } catch {
                // Users of the framework my develop their own responses here, such as notifying the user if appropriate
            }
        }
    }
}
