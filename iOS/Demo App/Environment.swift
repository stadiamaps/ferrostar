//
//  Environment.swift
//  Demo App
//
//  Created by Ian Wagner on 2023-05-15.
//

import Foundation
import SwiftUI

struct StadiaMapsApiKeyEnvironmentKey: EnvironmentKey {
    // You can sign up for a Stadia Maps account for free at https://client.stadiamaps.com/signup/
    // Lean how to create an API key here: https://docs.stadiamaps.com/authentication/#api-keys
    static let defaultValue: String = "YOUR_API_KEY"
}

extension EnvironmentValues {
    var stadiaMapsApiKey: String {
        get { self[StadiaMapsApiKeyEnvironmentKey.self] }
        set { self[StadiaMapsApiKeyEnvironmentKey.self] = newValue }
    }
}
