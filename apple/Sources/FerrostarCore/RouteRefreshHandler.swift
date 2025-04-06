import Foundation

public protocol RouteRefreshHandler {
    func onRouteRefresh(core: FerrostarCore, tripState: TripState) -> CorrectiveAction
}
