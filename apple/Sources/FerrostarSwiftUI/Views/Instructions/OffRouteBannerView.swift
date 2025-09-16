import SwiftUI

struct OffRouteBannerView: View {
    private let message: String
    @Binding private var size: CGSize
    private let primaryRowTheme: InstructionRowTheme

    init(
        message: String = String(localized: "Return to the Route"),
        primaryRowTheme: InstructionRowTheme = DefaultInstructionRowTheme(),
        size: Binding<CGSize> = .constant(.zero)
    ) {
        self.message = message
        self.primaryRowTheme = primaryRowTheme
        _size = size
    }

    var body: some View {
        HStack {
            Text(message)
                .font(.title2.bold())
                .padding()

            Spacer()
        }
        .background(primaryRowTheme.backgroundColor)
        .foregroundStyle(primaryRowTheme.iconTintColor)
        .cornerRadius(12)
        .shadow(radius: 12)
        .overlay(
            GeometryReader { geometry in
                Color.clear.onAppear {
                    size = geometry.size
                }.onChange(of: geometry.size) { newValue in
                    size = newValue
                }.onDisappear {
                    size = .zero
                }
            }
        )
    }
}
