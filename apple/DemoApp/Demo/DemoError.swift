import Foundation

enum DemoError: Error {
    case sessionNotInProgress
    case invalidOrigin
    case noOrigin
    case invalidDestination
    case noRoutesLoaded
    case noFirstRoute
    case invalidState(DemoAppState)
    case invalidCPRouteChoice
}

extension DemoError: LocalizedError {
    var errorDescription: String? {
        switch self {
        case .sessionNotInProgress:
            "The CPNavigationSession must be in progress before this action."
        case .invalidOrigin:
            "The origin point is invalid. Make sure you have a current user location."
        case .noOrigin:
            "There is no origin. Make sure you have a current user location."
        case .invalidDestination:
            "The destination is invalid."
        case .noRoutesLoaded:
            "No routes loaded"
        case .noFirstRoute:
            "Could not find a route in the list from the server"
        case let .invalidState(state):
            "Invalid state: \(state)"
        case .invalidCPRouteChoice:
            "Invalid CPRouteChoice"
        }
    }
}
