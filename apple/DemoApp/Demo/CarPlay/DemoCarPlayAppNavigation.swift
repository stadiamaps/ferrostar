import SwiftUI

struct DemoCarPlayAppNavigation: View {
    @State private var navController = DemoCarPlayNavController()

    var body: some View {
        NavigationStack(path: $navController.path) {
            VStack {
                Text("Ferrostar Demo")
            }
            .navigationDestination(for: DemoCarPlayScene.self) { scene in
                switch scene {
                case .search:
                    DemoCarPlaySearchView()
                case .navigation:
                    DemoCarPlayNavigationView()
                }
            }
        }
        .onAppear {
            navController.navigate(to: .navigation)

            // Attach the interface controller delegate to the car play session.
            // This allows linking swiftui behaviors & models to certain template change events.
            // It's hacky, but allows easily hosting a scene's model into it's appeared scope.
            //
            // Using this as inspiration, a fancy combined SwiftUI NavigationPath + CPInterfaceController delegate could
            // finely control the template, underlying view (visible only on maps templates) and the associated view
            // model.
            CarPlaySession.shared.registerDelegate(navController)
        }
        .environment(\.carPlayNavController, navController)
    }
}
