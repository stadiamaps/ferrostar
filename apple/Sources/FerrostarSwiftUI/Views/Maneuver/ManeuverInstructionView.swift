import CoreLocation
import FerrostarCoreFFI
import MapKit
import SwiftUI

/// A Customizable Maneuver Instruction View.
public struct ManeuverInstructionView<ManeuverView: View>: View {
    private let text: String
    private let distanceToNextManeuver: CLLocationDistance?
    private let distanceFormatter = MKDistanceFormatter()
    private let maneuverView: ManeuverView
    private let theme: InstructionRowTheme

    /// Initialize a manuever instruction view that includes a custom leading view or icon..
    /// As an HStack, this view automatically corrects for .rightToLeft languages.
    ///
    /// - Parameters:
    ///   - text: The maneuver instruction.
    ///   - distanceToNextManeuver: The distance to the next step.
    ///   - maneuverView: The custom view representing the maneuver.
    ///   - theme: The instruction row theme specifies attributes like colors and fonts for the row.
    public init(
        text: String,
        distanceToNextManeuver: CLLocationDistance? = nil,
        theme: InstructionRowTheme = DefaultInstructionRowTheme(),
        @ViewBuilder maneuverView: () -> ManeuverView = { EmptyView() }
    ) {
        self.text = text
        self.distanceToNextManeuver = distanceToNextManeuver
        self.maneuverView = maneuverView()
        self.theme = theme
    }

    public var body: some View {
        HStack {
            maneuverView
                .frame(width: 64)
                .foregroundColor(theme.iconTintColor)

            VStack(alignment: .leading) {
                if let distanceToNextManeuver {
                    Text("\(distanceFormatter.string(fromDistance: distanceToNextManeuver))")
                        .font(theme.distanceFont)
                        .foregroundStyle(theme.distanceColor)
                }

                Text(text)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                    .font(theme.instructionFont)
                    .foregroundStyle(theme.instructionColor)
            }

            Spacer(minLength: 0)
        }
    }
}

#Preview {
    VStack {
        ManeuverInstructionView(
            text: "Turn Right on Road Ave.",
            distanceToNextManeuver: 24140.16
        ) {
            Image(systemName: "car.circle.fill")
                .symbolRenderingMode(.multicolor)
                .resizable()
                .scaledToFit()
                .frame(width: 32)
        }
        .font(.title)

        ManeuverInstructionView(
            text: "Merge Left",
            distanceToNextManeuver: 152.4
        ) {
            ManeuverImage(maneuverType: .merge, maneuverModifier: .left)
                .frame(width: 24)
        }
        .font(.body)
        .foregroundColor(.blue)

        // Demonstrate a Right to Left
        ManeuverInstructionView(
            text: "ادمج يسارًا"
        ) {
            ManeuverImage(maneuverType: .merge, maneuverModifier: .left)
                .frame(width: 24)
        }
        .environment(\.layoutDirection, .rightToLeft)
    }
}
