import Foundation
import UniFFI

public protocol RouteRefreshHandler {
    func onRouteRefresh(core: FerrostarCore, tripState: TripState) -> CorrectiveAction
}
