import SwiftUI

public struct NavigationUIButtonStyle: ButtonStyle {
    /// The ferrostar button style.
    public init() {}

    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding()
            .background(Color(.systemBackground))
            .foregroundStyle(.primary)
            .clipShape(Capsule())
            .frame(minWidth: 52, minHeight: 52)
            .shadow(radius: 8)
    }
}

public struct NavigationUIButton<Label: View>: View {
    let action: () -> Void
    let label: Label

    /// The basic Ferrostar SwiftUI button style.
    ///
    /// - Parameters:
    ///   - action: The action the button performs on tap.
    ///   - label: The label subview.
    public init(action: @escaping () -> Void, label: () -> Label) {
        self.action = action
        self.label = label()
    }

    public var body: some View {
        Button {
            action()
        } label: {
            label
        }
        .buttonStyle(NavigationUIButtonStyle())
    }
}

#Preview {
    VStack {
        NavigationUIButton {} label: {
            Image(systemName: "location")
        }

        NavigationUIButton {} label: {
            Text("Start Navigation")
        }
    }
    .padding()
    .background(Color.green)
}
