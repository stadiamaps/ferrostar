//
//  UniFFIExtensions.swift
//  FerrostarCore
//
//  Created by Patrick Wolowicz on 27.09.24.
//

import FerrostarCoreFFI
import FerrostarCore
import CoreLocation
import MapLibre

extension FerrostarCore {
    
    public var isNavigating: Bool {
        return self.state?.isNavigating ?? false
    }
}

extension NavigationState {
    public var isNavigating: Bool {
        if case .navigating = tripState {
            return true
        } else {
            return false
        }
    }
}

public extension Waypoint {
    init(coordinate: CLLocationCoordinate2D, kind: WaypointKind = .via) {
        self.init(coordinate: GeographicCoordinate(lat: coordinate.latitude, lng: coordinate.longitude), kind: kind)
    }
    
    var cLCoordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: self.coordinate.lat, longitude: self.coordinate.lng)
    }
}

public extension Route {
    var duration: TimeInterval {
        // add together all routeStep durations
        return self.steps.reduce(0) { $0 + $1.duration }
    }
}

extension Route: Identifiable {
    public var id: Int {
        return self.hashValue
    }
    
}

extension BoundingBox {
    public var mlnCoordinateBounds: MLNCoordinateBounds {
        return MLNCoordinateBounds(sw: self.sw.clLocationCoordinate2D, ne: self.ne.clLocationCoordinate2D)
    }
}

extension [GeographicCoordinate] {
    public var clLocationCoordinate2Ds: [CLLocationCoordinate2D] {
        return self.map(\.clLocationCoordinate2D)
    }
}
