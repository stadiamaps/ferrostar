import SwiftUI

struct OffRouteBannerView: View {
    private let primaryRowTheme: InstructionRowTheme

    let message: String

    init(
        message: String = String(localized: "Return to the Route"),
        primaryRowTheme: InstructionRowTheme = DefaultInstructionRowTheme()
    ) {
        self.message = message
        self.primaryRowTheme = primaryRowTheme
    }

    var body: some View {
        HStack {
            Image(systemName: "")

            Text(message)
                .font(.title2.bold())
                .padding()

            Spacer()
        }
        .background(primaryRowTheme.backgroundColor)
        .foregroundStyle(primaryRowTheme.iconTintColor)
        .cornerRadius(12)
        .shadow(radius: 12)
    }
}
