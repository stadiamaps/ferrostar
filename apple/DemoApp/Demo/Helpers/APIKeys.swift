import Foundation

class APIKeys {
    static let shared = APIKeys()

    let stadiaMapsAPIKey: String

    init() {
        guard let path = Bundle.main.path(forResource: "API-Keys", ofType: "plist"),
              let data = FileManager.default.contents(atPath: path),
<<<<<<< HEAD
              let dict = try! PropertyListSerialization.propertyList(from: data, options: .mutableContainersAndLeaves, format: nil) as? [String: Any]
=======
              let dict = try! PropertyListSerialization.propertyList(
                  from: data,
                  options: .mutableContainersAndLeaves,
                  format: nil
              ) as? [String: Any]
>>>>>>> 746c43483e74319176f21e1fe96b78c038215c0b
        else {
            fatalError("API-Keys.plist not found or invalid.")
        }

        stadiaMapsAPIKey = (dict["STADIAMAPS_API_KEY"] as! String)
    }
}
