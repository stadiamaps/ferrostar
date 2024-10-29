import SwiftUI

public struct CurrentRoadNameView: View {
    let currentRoadName: String?
    let theme: any RoadNameViewTheme
    var padding: EdgeInsets = EdgeInsets(top: 12, leading: 12, bottom: 12, trailing: 12)
    var shape: AnyShape = AnyShape(RoundedRectangle(cornerRadius: 48))
    var borderWidth: CGFloat = 1

    public init(currentRoadName: String?,
                theme: any RoadNameViewTheme = DefaultRoadNameViewTheme())
    {
        self.currentRoadName = currentRoadName
        self.theme = theme
    }

    public var body: some View {
        if let currentRoadName {
            Text(currentRoadName)
                .font(theme.textFont)
                .foregroundStyle(theme.textColor)
                .padding(padding)
                .background(theme.backgroundColor)
                .clipShape(shape)
                .overlay(shape.stroke(theme.borderColor, lineWidth: borderWidth))
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
    func shape<T: Shape>(_ shape: T) -> Self {
        var newSelf = self
        newSelf.shape = AnyShape(shape)
        return newSelf
    }

    /// Sets the width of the border stroke.
    func borderWidth(_ width: CGFloat) -> Self {
        var newSelf = self
        newSelf.borderWidth = borderWidth
        return newSelf
    }
}

#Preview {
    CurrentRoadNameView(currentRoadName: "Sesame Street")
}
