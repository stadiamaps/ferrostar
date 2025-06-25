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
