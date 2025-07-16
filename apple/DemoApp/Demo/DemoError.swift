import Foundation

enum DemoError: Error {
    case invalidOrigin
    case noOrigin
    case invalidDestination
    case noRoutesLoaded
    case noFirstRoute
    case invalidState(DemoAppState)
    case invalidCPRouteChoice
}
