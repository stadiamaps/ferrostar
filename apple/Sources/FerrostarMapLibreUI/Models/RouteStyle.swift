import UIKit
import MapLibreSwiftDSL

/// A customizable style for the route and it's casing.
public protocol RouteStyle {
    var color: UIColor { get }
    var casingColor: UIColor { get }
    // TODO: Add route & route casing scaling/diameter
    var lineCap: LineCap { get }
    var lineJoin: LineJoin { get }
}

public struct ActiveRouteStyle: RouteStyle {
    public let color: UIColor = .systemBlue
    public let casingColor: UIColor = .white
    public let lineCap: LineCap = .round
    public let lineJoin: LineJoin = .round
    public init() { /* No def */ }
}

public struct TravelledRouteStyle: RouteStyle {
    public var color: UIColor = .systemGray
    public var casingColor: UIColor = .white
    public let lineCap: LineCap = .round
    public let lineJoin: LineJoin = .round
    public init() { /* No def */ }
}
