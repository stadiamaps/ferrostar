//
//  DemoApp.swift
//  iOS Demo
//
//  Created by Ian Wagner on 2023-10-09.
//

import SwiftUI

let stadiaMapsAPIKey = Bundle.main.infoDictionary!["STADIAMAPS_API_KEY"] as! String

@main
struct DemoApp: App {
    var body: some Scene {
        WindowGroup {
            NavigationView()
        }
    }
}
