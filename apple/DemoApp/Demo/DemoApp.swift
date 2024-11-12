import SwiftUI

@main
struct DemoApp: App {
    @UIApplicationDelegateAdaptor private var appDelegate: AppDelegate
    
    var body: some Scene {
        WindowGroup {
            DemoNavigationView()
        }
    }
}

// We still need a minimal AppDelegate for CarPlay
class AppDelegate: NSObject, UIApplicationDelegate {
    
}
