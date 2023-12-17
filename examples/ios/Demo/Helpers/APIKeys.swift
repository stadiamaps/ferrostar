//
//  APIKeys.swift
//  Demo
//
//  Created by Jacob Fielding on 12/17/23.
//

import Foundation

class APIKeys {
    
    static let shared = APIKeys()
    
    let stadiaMapsAPIKey: String
    
    init() {
        guard let path = Bundle.main.path(forResource: "API-Keys", ofType: "plist"),
              let data = FileManager.default.contents(atPath: path),
              let dict = try! PropertyListSerialization.propertyList(from: data, options: .mutableContainersAndLeaves, format: nil) as? [String: Any] else {
            fatalError("API-Keys.plist not found or invalid.")
        }
        
        stadiaMapsAPIKey = (dict["STADIAMAPS_API_KEY"] as! String)
    }
}
