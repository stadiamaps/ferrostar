//
//  ContentView.swift
//  Ferrostar
//
//  Created by Ian Wagner on 2023-04-28.
//

import SwiftUI
import FerrostarCore

struct ContentView: View {
    var body: some View {
        VStack {
            let core = FerrostarCore()
            Text("Hello, world! 🦀 says 2 + 2 = \(core.add(2, 2))")
        }
        .padding()
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
