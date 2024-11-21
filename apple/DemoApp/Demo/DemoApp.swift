import SwiftUI

// This AppDelegate setup is an easy way to share your environment with CarPlay
class DemoAppDelegate: NSObject, UIApplicationDelegate {
    let appEnvironment = AppEnvironment()
}

@main
struct DemoApp: App {
    
    @UIApplicationDelegateAdaptor(DemoAppDelegate.self) private var appDelegate: DemoAppDelegate
    
    var body: some Scene {
        WindowGroup {
            DemoNavigationView()
                .environmentObject(appDelegate.appEnvironment)
                .environmentObject(appDelegate.appEnvironment.ferrostarCore)
        }
    }
}
