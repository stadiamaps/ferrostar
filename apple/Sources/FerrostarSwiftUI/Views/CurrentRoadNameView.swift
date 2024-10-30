import SwiftUI

public protocol CurrentRoadNameViewHost {
    var currentRoadNameView: AnyView? { get set }
}

public extension CurrentRoadNameViewHost where Self: View {
    func navigationCurrentRoadView(@ViewBuilder currentRoadNameViewBuilder: () -> some View) -> Self {
        var newSelf = self
        newSelf.currentRoadNameView = AnyView(currentRoadNameViewBuilder())
        return newSelf
    }
}

public struct CurrentRoadNameView: View {
    let currentRoadName: String?
    var theme: any RoadNameViewTheme = DefaultRoadNameViewTheme()
    var padding: EdgeInsets = .init(top: 12, leading: 12, bottom: 12, trailing: 12)
    var shape: AnyShape = .init(RoundedRectangle(cornerRadius: 48))
    var borderWidth: CGFloat = 2

    public init(currentRoadName: String?) {
        self.currentRoadName = currentRoadName
    }

    public var body: some View {
        if let currentRoadName, !currentRoadName.isEmpty {
            Text(currentRoadName)
                .font(theme.textFont)
                .foregroundStyle(theme.textColor)
                .padding(padding)
                .background(theme.backgroundColor)
                .clipShape(shape)
                .overlay(shape.stroke(theme.borderColor, lineWidth: borderWidth).clipShape(shape))
        }
    }
}

public extension CurrentRoadNameView {
    /// Sets the interior padding (expanding the buffer around the text).
    func padding(_ padding: EdgeInsets) -> Self {
        var newSelf = self
        newSelf.padding = padding
        return newSelf
    }

    /// Sets the shape for clipping and border stroke.
    func shape(_ shape: some Shape) -> Self {
        var newSelf = self
        newSelf.shape = AnyShape(shape)
        return newSelf
    }

    /// Sets the width of the border stroke.
    func borderWidth(_ borderWidth: CGFloat) -> Self {
        var newSelf = self
        newSelf.borderWidth = borderWidth
        return newSelf
    }

    /// Sets the width of the border stroke.
    func theme(_ theme: any RoadNameViewTheme) -> Self {
        var newSelf = self
        newSelf.theme = theme
        return newSelf
    }
}

#Preview {
    CurrentRoadNameView(currentRoadName: "Sesame Street")
}
