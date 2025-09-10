import SwiftUI

@main
struct DemoApp: App {
    @State private var model = demoModel

    var body: some Scene {
        WindowGroup {
            ZStack {
                if let model {
                    if let errorMessage = model.errorMessage {
                        ContentUnavailableView(
                            errorMessage, systemImage: "network.slash",
                            description: Text("error navigating.")
                        )
                        .onTapGesture {
                            model.errorMessage = nil
                        }
                    } else {
                        DemoNavigationView(model: model)
                    }
                } else {
                    ContentUnavailableView(
                        "cannot create model", systemImage: "network.slash",
                        description: Text("Unable to create model.")
                    )
                }
            }
        }
    }
}
