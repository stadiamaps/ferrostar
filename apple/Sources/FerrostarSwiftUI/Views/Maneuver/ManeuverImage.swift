import FerrostarCoreFFI
import SwiftUI

/// A resizable image view for a Maneuver type and modifier combination.
public struct ManeuverImage: View {
    let name: ManeuverImageName

    /// A maneuver image using `mapbox-directions` icons for common manueuvers.
    ///
    /// This view will be empty if the icon does not exist for the given maneuver type and modifier.
    ///
    /// - Parameters:
    ///   - maneuverType: The maneuver type defines the behavior.
    ///   - maneuverModifier: The maneuver modifier defines the direction.
    ///   - drivingSide: The road's driving side. Defaults to right-hand traffic.
    public init(maneuverType: ManeuverType,
                maneuverModifier: ManeuverModifier?,
                drivingSide: DrivingSide = .right)
    {
        name = ManeuverImageName(maneuverType: maneuverType,
                                 maneuverModifier: maneuverModifier,
                                 drivingSide: drivingSide)
    }

    public var body: some View {
        Image(name.value, bundle: .module)
            .resizable()
            .aspectRatio(contentMode: .fit)
            .scaleEffect(x: name.shouldMirror ? -1 : 1, y: 1)
    }
}

#Preview {
    VStack {
        ManeuverImage(maneuverType: .turn, maneuverModifier: .right)
            .frame(width: 128, height: 128)

        ManeuverImage(maneuverType: .fork, maneuverModifier: .left)
            .frame(width: 32)

        ManeuverImage(maneuverType: .rotary, maneuverModifier: .slightRight)

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
