import FerrostarCoreFFI
import SwiftUI

/// A resizable image view for a Maneuver type and modifier combination.
public struct ManeuverImage: View {
    let maneuverType: ManeuverType
    let maneuverModifier: ManeuverModifier?

    /// A maneuver image using `mapbox-directions` icons for common manueuvers.
    ///
    /// This view will be empty if the icon does not exist for the given maneuver type and modifier.
    ///
    /// - Parameters:
    ///   - maneuverType: The maneuver type defines the behavior.
    ///   - maneuverModifier: The maneuver modifier defines the direction.
    public init(maneuverType: ManeuverType,
                maneuverModifier: ManeuverModifier?)
    {
        self.maneuverType = maneuverType
        self.maneuverModifier = maneuverModifier
    }

    public var body: some View {
        Image(maneuverImageName, bundle: .module)
            .resizable()
            .aspectRatio(contentMode: .fit)
    }

    var maneuverImageName: String {
        let parsedModifier = maneuverModifier?.iconKey.replacingOccurrences(of: " ", with: "_")

        var imageName = maneuverType.iconKey.replacingOccurrences(of: " ", with: "_")
        if let parsedModifier {
            imageName += "_\(parsedModifier)"
        }

        return imageName
    }
}

#Preview {
    VStack {
        ManeuverImage(maneuverType: .turn, maneuverModifier: .right)
            .frame(width: 128, height: 128)

        ManeuverImage(maneuverType: .fork, maneuverModifier: .left)
            .frame(width: 32)

        ManeuverImage(maneuverType: .merge, maneuverModifier: .slightLeft)
            .frame(width: 92)
            .foregroundColor(.blue)

        // A ManeuverImage for a combination that doesn't have an icon.
        ManeuverImage(maneuverType: .arrive, maneuverModifier: .slightLeft)
            .frame(width: 92)
            .foregroundColor(.white)
            .background(Color.green)
    }
}
