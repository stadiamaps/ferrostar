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

let style = URL(string: "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=\(APIKeys.shared.stadiaMapsAPIKey)")!

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
    
    private let locationManager: LiveLocationProvider
    private let ferrostarCore: FerrostarCore
//    @StateObject private var locationManager = LiveLocationProvider(activityType: .otherNavigation)
//    @State private var ferrostarCore: FerrostarCore =
    @State private var isFetchingRoutes = false
    @State private var routes: [Route]?
    @State private var errorMessage: String?

    init() {
        locationManager = LiveLocationProvider(activityType: .otherNavigation)
        ferrostarCore = FerrostarCore(
            valhallaEndpointUrl: URL(string: "https://api.stadiamaps.com/route/v1?api_key=\(APIKeys.shared.stadiaMapsAPIKey)")!,
            profile: "pedestrian",
            locationManager: locationManager)
    }
    
    var body: some View {
        let locationServicesEnabled = locationManager.authorizationStatus == .authorizedAlways 
            || locationManager.authorizationStatus == .authorizedWhenInUse

        NavigationStack {
            
            
            // TODO: Make navigationState optional on NavigationMapView
            NavigationMapView(lightStyleURL: style,
                              darkStyleURL: style,
                              navigationState: ferrostarCore.observableState)
            .overlay(alignment: .bottomLeading) {
                VStack {
                    NavigationLink("Settings") {
                        Text("Settings")
                    }
                    
                    Spacer()
                    
                    if let location = locationManager.lastLocation {
                        
                        Text("±\(Int(location.horizontalAccuracy))m accuracy")
                            .foregroundColor(.white)
                            .padding(.all, 8)
                            .background(Color.black.opacity(0.7).clipShape(.buttonBorder, style: FillStyle()))
                    
                        Button("Start Navigation") {
                            Task {
                                do {
                                    try await startNavigation()
                                } catch {
                                    print("Error starting navigation: \(error)")
                                }
                            }
                        }
                        .padding(.all, 8)
                        .background(
                            Color.white
                                .clipShape(.buttonBorder, style: FillStyle())
                                .shadow(radius: 4)
                        )
                    } else {
                        // TODO: No location - enable location services.
                        Button("Enable Location Services") {
                            // TODO:
                        }
                    }
                }
                .padding()
            }
        }
    }
    
    func startNavigation() async throws {
        guard let userLocation = locationManager.lastLocation else {
            print("No user location")
            return
        }
        
        let waypoints = locations.map {
            CLLocationCoordinate2D(latitude: $0.location.latitude,
                                   longitude: $0.location.longitude)
        }
        
        routes = try await ferrostarCore.getRoutes(initialLocation: userLocation, waypoints: waypoints)
        
        guard let route = routes?.first else {
            print("No route")
            return
        }
        
        try ferrostarCore.startNavigation(
            route: route,
            stepAdvance: .relativeLineStringDistance(minimumHorizontalAccuracy: 32, automaticAdvanceDistance: 10))
    }
    
}

#Preview {
    NavigationView()
}
