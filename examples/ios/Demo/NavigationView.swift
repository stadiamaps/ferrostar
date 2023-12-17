//
//  NavigationView.swift
//  iOS Demo
//
//  Created by Ian Wagner on 2023-10-09.
//

import SwiftUI
import CoreLocation
import FerrostarCore
import FerrostarMapLibreUI

let style = URL(string: "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=\(stadiaMapsAPIKey)")!

struct LocationIdentifier : Identifiable, Equatable, Hashable {
    static func == (lhs: LocationIdentifier, rhs: LocationIdentifier) -> Bool {
        lhs.id == rhs.id
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }

    let id = UUID()
    let name: String
    let location: CLLocationCoordinate2D
}

let locations = [
    LocationIdentifier(name: "Cupertino HS", location: CLLocationCoordinate2D(latitude: 37.31910, longitude: -122.01018)),
]

struct NavigationView: View {
    @StateObject private var locationManager = LiveLocationProvider(activityType: .otherNavigation)
    @State private var ferrostarCore: FerrostarCore!
    @State private var isFetchingRoutes = false
    @State private var routes: [Route]?
    @State private var errorMessage: String?

    var body: some View {
        let locationServicesEnabled = locationManager.authorizationStatus == .authorizedAlways 
            || locationManager.authorizationStatus == .authorizedWhenInUse

        NavigationStack {
            NavigationLink("Settings") {
                Text("Settings")
            }

            // TODO: Make navigationState optional on NavigationMapView
            NavigationMapView(lightStyleURL: style,
                              darkStyleURL: style,
                              navigationState: .modifiedPedestrianExample(droppingNWaypoints: 2))
            .overlay(alignment: .bottomLeading) {
                if let location = locationManager.lastLocation {
                    VStack {
                        Text("±\(Int(location.horizontalAccuracy))m accuracy")
                            .foregroundColor(.white)
                    }
                    .padding(.all, 8)
                    .background(Color.black.opacity(0.7).clipShape(.buttonBorder, style: FillStyle()))
                }
            }
        }
    }
}

#Preview {
    NavigationView()
}
