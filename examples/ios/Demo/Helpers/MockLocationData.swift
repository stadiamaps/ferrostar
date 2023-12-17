//
//  MockLocationData.swift
//  Ferrostar Demo
//
//  Created by Jacob Fielding on 12/17/23.
//

import Foundation
import CoreLocation

struct LocationIdentifier : Identifiable, Equatable, Hashable {
    static func == (lhs: LocationIdentifier, rhs: LocationIdentifier) -> Bool {
        lhs.id == rhs.id
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }

    let id = UUID()
    let name: String
    let coordinate: CLLocationCoordinate2D
}

let locations = [
    LocationIdentifier(name: "Cupertino HS", coordinate: CLLocationCoordinate2D(latitude: 37.31910, longitude: -122.01018)),
]


