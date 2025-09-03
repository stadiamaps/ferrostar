import CoreLocation

// Temporary measure until we transition the demo app to Swift 6.
// See https://github.com/stadiamaps/ferrostar/issues/164#issuecomment-3037767025
@preconcurrency import FerrostarCoreFFI
import Foundation

enum DemoAppState: Equatable, Hashable, CustomStringConvertible {
    case idle
    case routes(routes: [Route])
    case selectedRoute(Route)
    case navigating

    var description: String {
        switch self {
        case .idle:
            "Idle"
        case let .routes(routes: routes):
            "Routes: \(routes.count)"
        case let .selectedRoute(route):
            "Route: \(route)"
        case .navigating:
            "Navigating"
        }
    }

    var buttonText: String? {
        switch self {
        case .idle, .navigating:
            nil
        case .routes:
            "Select Route 1"
        case .selectedRoute:
            "Start Navigation"
        }
    }
}
