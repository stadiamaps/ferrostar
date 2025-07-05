import CoreLocation

// Temporary measure until we transition the demo app to Swift 6.
// See https://github.com/stadiamaps/ferrostar/issues/164#issuecomment-3037767025
@preconcurrency import FerrostarCoreFFI
import Foundation

enum DemoAppState: CustomStringConvertible {
    case idle
    case destination(CLLocationCoordinate2D)
    case routes([Route])
    case selectedRoute(Route)
    case navigating

    var description: String {
        switch self {
        case .idle:
            "Idle"
        case let .destination(location):
            "Destination: (\(location)"
        case let .routes(routes):
            "Routes: \(routes.count)"
        case let .selectedRoute(route):
            "Route: \(route)"
        case .navigating:
            "Navigating"
        }
    }

    var buttonText: String {
        switch self {
        case .idle:
            "Choose Destination"
        case .destination:
            "Load Routes"
        case .routes:
            "Choose Route"
        case .selectedRoute:
            "Start Navigation"
        case .navigating:
            "Stop"
        }
    }
}

extension DemoAppState {
    func setDestination(_ destination: CLLocationCoordinate2D) throws -> DemoAppState {
        switch self {
        case .idle:
            guard destination != kCLLocationCoordinate2DInvalid else { throw DemoError.invalidDestination }
            return .destination(destination)
        case .destination(_), .routes(_), .selectedRoute(_), .navigating:
            throw DemoError.invalidState(self)
        }
    }
}
