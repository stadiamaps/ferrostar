//
//  ConfigurationView.swift
//  Ferrostar Demo
//
//  Created by Jacob Fielding on 12/6/23.
//

import SwiftUI

struct ConfigurationView: View {
    
    var body: some View {
        VStack(spacing: 15) {
//            Image(systemName: locationServicesEnabled ? "location.fill" : "location.slash.fill")
//                .imageScale(.large)
//                .foregroundStyle(.tint)
//            Text("Location services \(locationServicesEnabled ? "available" : "unavailable")")
//            Text("\(locationManager.lastLocation?.description ?? "Location unknown")")
//                .fontDesign(.monospaced)
//
//            if locationServicesEnabled && locationManager.lastLocation != nil && !isFetchingRoutes {
//                ForEach(locations) { loc in
//                    Button(loc.name) {
//                        let userLocation = locationManager.lastLocation!
//                        if ferrostarCore == nil {
//                            ferrostarCore = FerrostarCore(valhallaEndpointUrl: URL(string: "https://api.stadiamaps.com/route/v1?api_key=\(stadiaMapsAPIKey)")!, profile: "pedestrian", locationManager: locationManager)
//                        }
//
//                        Task {
//                            do {
//                                routes = try await ferrostarCore.getRoutes(initialLocation: userLocation, waypoints: [loc.location])
//
//                                // TODO: Show a preview with selectable routes
//                                try ferrostarCore.startNavigation(route: routes!.first!, stepAdvance: .relativeLineStringDistance(minimumHorizontalAccuracy: 32, automaticAdvanceDistance: 10))
//
//                                errorMessage = nil
//                            } catch {
//                                errorMessage = "Error: \(error)"
//                            }
//                        }
//                    }
//                }
//            }
        }
        .navigationTitle("Config")
    }
}

#Preview {
    ConfigurationView()
}
