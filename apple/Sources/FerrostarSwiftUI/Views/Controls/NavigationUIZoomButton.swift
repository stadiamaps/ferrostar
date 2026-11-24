import SwiftUI

public struct NavigationUIZoomButton: View {
    let onZoomIn: () -> Void
    let onZoomOut: () -> Void

    public var body: some View {
        VStack(spacing: 0) {
            Button(
                action: onZoomIn,
                label: {
                    Image(systemName: "plus")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 18, height: 18)
                }
            )
            .padding()
            .accessibilityLabel(Text("Zoom in", bundle: .module,
                                     comment: "Accessibility label for the button that increases map zoom."))

            Divider()
                .frame(width: 52)

            Button(
                action: onZoomOut,
                label: {
                    Image(systemName: "minus")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 18, height: 18)
                }
            )
            .padding()
            .accessibilityLabel(Text("Zoom out", bundle: .module,
                                     comment: "Accessibility label for the button that decreases map zoom."))
        }
        .foregroundColor(.primary)
        .background(Color(.systemBackground))
        .clipShape(Capsule())
        .shadow(radius: 8)
    }
}

#Preview {
    VStack {
        NavigationUIZoomButton(
            onZoomIn: {},
            onZoomOut: {}
        )
    }
    .padding()
    .background(Color.green)
}
