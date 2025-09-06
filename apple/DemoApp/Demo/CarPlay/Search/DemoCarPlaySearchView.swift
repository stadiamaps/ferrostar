import SwiftUI

struct DemoCarPlaySearchView: View {
    @Environment(\.carPlayNavController) var navController
    @State var model = DemoCarPlaySearchModel()

    // This empty view hosts the view model under the CPSearchTemplate.

    var body: some View {
        Text("")
            .onAppear {
                Task {
                    try? await model.onAppear(navController)
                }
            }
            .navigationBarHidden(true)
            .navigationBarBackButtonHidden(true)
    }
}
