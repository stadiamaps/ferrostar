import FerrostarCoreFFI
import Foundation

public extension FerrostarCore {
    override var description: String {
        "Core: [route: \(route != nil ? route!.description : "none") state: \(state != nil ? state!.description : "none")]"
    }
}

extension NavigationState: CustomStringConvertible {
    public var description: String {
        "NavState: \(tripState)"
    }
}

extension Waypoint: CustomStringConvertible {
    public var description: String {
        "Waypoint: \(coordinate) kind: \(kind)"
    }
}

extension TripState: CustomStringConvertible {
    public var description: String {
        switch self {
        case let .idle(userLocation):
            "idle: \(userLocation != nil ? "\(userLocation!.coordinates)" : "none")"
        case let .navigating(_, _, snappedUserLocation, _, _, _, _, _, visualInstruction, _, _):
            "navigating: \(snappedUserLocation.coordinates) instruction: \(visualInstruction != nil ? visualInstruction!.primaryContent.text : "none")"
        case let .complete(userLocation, _):
            "complete: \(userLocation.coordinates)"
        }
    }
}

extension WaypointKind: CustomStringConvertible {
    public var description: String {
        switch self {
        case .break:
            "break"
        case .via:
            "via"
        }
    }
}

extension RouteStep: CustomStringConvertible {
    public var description: String {
        "RouteStep: \(instruction)"
    }
}

extension Route: CustomStringConvertible {
    public var description: String {
        "Route [distance \(distance) waypoints: \(waypoints.map(\.description).joined(separator: ",")) steps: \(steps.map(\.description).joined(separator: ","))]"
    }
}
