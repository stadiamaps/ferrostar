//
//  Navigation Delegate.swift
//  Ferrostar Demo
//
//  Created by Ian Wagner on 2024-02-25.
//

import CoreLocation
import FerrostarCore

/// Not all navigation apps will require a navigation delegate. In fact, we hope that most don't!
///
/// In case you do though, this sample implementation shows what you'll need to get started
/// by re-implementing the default behaviors of the core.
class NavigationDelegate: FerrostarCoreDelegate {
    func core(_ core: FerrostarCore, correctiveActionForDeviation deviationInMeters: Double) -> CorrectiveAction {
        // If the user is off course, we'll try to calculate a new route using the remaining waypoints.
        if let remainingWaypoints = core.state?.remainingWaypoints {
            return .getNewRoutes(waypoints: remainingWaypoints)
        } else {
            return .doNothing
        }
    }
    
    func core(_ core: FerrostarCore, loadedAlternateRoutes routes: [Route]) {
        // Automatically accept the first new route if the framework was calculating a new route
        // due to the user being off course.
        // Pretty sensible default.
        if core.state?.isCalculatingNewRoute ?? false,
           let route = routes.first {
            do {
                try core.startNavigation(
                    route: route,
                    stepAdvance: .relativeLineStringDistance(minimumHorizontalAccuracy: 32, automaticAdvanceDistance: 10),
                    routeDeviationTracking: .staticThreshold(minimumHorizontalAccuracy: 25, maxAcceptableDeviation: 20))
            } catch {
                // Users of the framework my develop their own responses here, such as notifying the user if appropriate
            }
        }
    }
}
