import SwiftUI

public struct CurrentRoadNameView: View {
    let currentRoadName: String?
    let theme: any RoadNameViewTheme

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
                .padding(.leading, 12)
                .padding(.trailing, 12)
                .padding(.vertical, 8)
                .background(theme.backgroundColor)
                .clipShape(.rect(cornerRadius: 48))
                .overlay(RoundedRectangle(cornerRadius: 48).stroke(theme.borderColor, lineWidth: 1))
        }
    }
}

#Preview {
    CurrentRoadNameView(currentRoadName: "Sesame Street")
}
