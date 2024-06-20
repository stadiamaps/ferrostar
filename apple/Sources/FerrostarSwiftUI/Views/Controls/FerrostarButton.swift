import SwiftUI

public struct FerrostarButtonStyle: ButtonStyle {
    /// The ferrostar button style.
    public init() {}

    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding()
            .background(Color(.systemBackground))
            .foregroundStyle(.primary)
            .clipShape(Capsule())
            .frame(minWidth: 52, minHeight: 52)
    }
}

public struct FerrostarButton<Label: View>: View {
    let action: () -> Void
    @ViewBuilder var label: () -> Label

    /// The basic Ferrostar SwiftUI button style.
    ///
    /// - Parameters:
    ///   - action: The action the button performs on tap.
    ///   - label: The label subview.
    public init(action: @escaping () -> Void, label: @escaping () -> Label) {
        self.action = action
        self.label = label
    }

    public var body: some View {
        Button {
            action()
        } label: {
            label()
        }
        .buttonStyle(FerrostarButtonStyle())
    }
}

#Preview {
    VStack {
        FerrostarButton {} label: {
            Image(systemName: "location")
        }

        FerrostarButton {} label: {
            Text("Start Navigation")
        }
    }
    .padding()
    .background(Color.green)
}
