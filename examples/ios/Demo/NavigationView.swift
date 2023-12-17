//
//  NavigationView.swift
//  Ferrostar Demo
//
//  Created by Ian Wagner on 2023-10-09.
//

import SwiftUI
import CoreLocation
import FerrostarCore
import FerrostarMapLibreUI

let style = URL(string: "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=\(APIKeys.shared.stadiaMapsAPIKey)")!

struct NavigationView: View {
    
    private let locationManager: LiveLocationProvider
    private let ferrostarCore: FerrostarCore
    
//    @StateObject private var locationManager = LiveLocationProvider(activityType: .otherNavigation)
//    @State private var ferrostarCore: FerrostarCore =
    
    @State private var isFetchingRoutes = false
    @State private var routes: [Route]?
    @State private var errorMessage: String? {
        didSet {
            Task {
                try await Task.sleep(nanoseconds: 10 * NSEC_PER_SEC)
                errorMessage = nil
            }
        }
    }

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
            NavigationMapView(
                lightStyleURL: style,
                darkStyleURL: style,
                navigationState: ferrostarCore.observableState,
                initialCamera: .center(locations.first!.coordinate, zoom: 14)
            )
            .overlay(alignment: .bottomLeading) {
                VStack {
                    HStack {
                        if isFetchingRoutes {
                            Text("Loading route...")
                                .font(.caption)
                                .padding(.all, 8)
                                .foregroundColor(.white)
                                .background(Color.black.opacity(0.7).clipShape(.buttonBorder, style: FillStyle()))
                        }
                        
                        if let errorMessage {
                            Text(errorMessage)
                                .font(.caption)
                                .padding(.all, 8)
                                .foregroundColor(.white)
                                .background(Color.red.opacity(0.7).clipShape(.buttonBorder, style: FillStyle()))
                                .onTapGesture {
                                    self.errorMessage = nil
                                }
                        }
                        
                        Spacer()
                        
                        NavigationLink {
                            ConfigurationView()
                        } label: {
                            Image(systemName: "gear")
                        }
                        .padding(.all, 8)
                        .background(
                            Color.white
                                .clipShape(.buttonBorder, style: FillStyle())
                                .shadow(radius: 4)
                        )
                    }
                    
                    Spacer()
                    
                    HStack {
                        Text(locationLabel)
                            .font(.caption)
                            .padding(.all, 8)
                            .foregroundColor(.white)
                            .background(Color.black.opacity(0.7).clipShape(.buttonBorder, style: FillStyle()))
                        
                        Spacer()
                        
                        if locationServicesEnabled {
                            Button("Start Navigation") {
                                Task {
                                    do {
                                        isFetchingRoutes = true
                                        try await startNavigation()
                                        isFetchingRoutes = false
                                    } catch {
                                        isFetchingRoutes = false
                                        errorMessage = "\(error.localizedDescription)"
                                    }
                                }
                            }
                            .disabled(routes?.isEmpty == true)
                            .padding(.all, 8)
                            .background(
                                Color.white
                                    .clipShape(.buttonBorder, style: FillStyle())
                                    .shadow(radius: 4)
                            )
                        } else {
                            Button("Enable Location Services") {
                                // TODO: enable location services.
                            }
                            .padding(.all, 8)
                            .background(
                                Color.white
                                    .clipShape(.buttonBorder, style: FillStyle())
                                    .shadow(radius: 4)
                            )
                        }
                    }
                }
                .padding()
                .padding(.bottom, 32)
                .task {
                    do {
                        try await getRoutes()
                    } catch {
                        errorMessage = "\(error)"
                    }
                }
            }
        }
    }
    
    // MARK: Conveniences
    
    func getRoutes() async throws {
        guard let userLocation = locationManager.lastLocation else {
            print("No user location")
            return
        }
        
        let waypoints = locations.map { $0.coordinate }
        
        routes = try await ferrostarCore.getRoutes(initialLocation: userLocation, waypoints: waypoints)
    }
    
    func startNavigation() async throws {
        guard let route = routes?.first else {
            print("No route")
            return
        }
        
        try ferrostarCore.startNavigation(
            route: route,
            stepAdvance: .relativeLineStringDistance(minimumHorizontalAccuracy: 32, automaticAdvanceDistance: 10))
    }
    
    var locationLabel: String {
        guard let userLocation = locationManager.lastLocation else {
            return "No location - authed as \(locationManager.authorizationStatus)"
        }
        
        return "±\(Int(userLocation.horizontalAccuracy))m accuracy"
    }

}

#Preview {
    NavigationView()
}
