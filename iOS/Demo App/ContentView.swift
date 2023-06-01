//
//  ContentView.swift
//  Ferrostar
//
//  Created by Ian Wagner on 2023-04-28.
//

import SwiftUI
import FFI
import FerrostarCore
import CoreLocation

struct ContentView: View {
    @Environment(\.stadiaMapsApiKey) var stadiaMapsApiKey


    var body: some View {
        let valhallaUrl = URL(string: "https://api.stadiamaps.com/route/v1?api_key=\(stadiaMapsApiKey)")!
        // Demo: user starts at the Blasco library and drives to Perry Monument
        // TODO: Initial milestone might throw a bunch of crap in one view, but we should quickly split out into multiple SwiftUI views with common components in a separate Swift package that's UI-focused
        let core = FerrostarCore(valhallaEndopointUrl: valhallaUrl, profile: "auto", locationManager: LiveLocationManager(activityType: .automotiveNavigation))

        // initialUserLocation: CLLocation(latitude: 42.13640615156194, longitude: -80.0863545447856), waypoints: [CLLocationCoordinate2D(latitude: 42.1550468279988, longitude: -80.0894362088786)]

        VStack {
            Text("Hello, world! (TODO: Make this a useful demo view)")
        }
        .padding()
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
