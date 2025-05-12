import SwiftUI

let appEnvironment = try! AppEnvironment()

@main
struct DemoApp: App {
    var body: some Scene {
        WindowGroup {
            DemoNavigationView()
                .environmentObject(appEnvironment)
                .environmentObject(appEnvironment.ferrostarCore)
        }
    }
}
