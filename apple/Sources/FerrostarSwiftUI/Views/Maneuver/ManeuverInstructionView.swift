import CoreLocation
import FerrostarCoreFFI
import MapKit
import SwiftUI

/// A generic maneuver instruction view.
///
/// This view allows for the user to specify an arbitrary View
/// as the first child of the HStack to enable custom iconography etc.
/// If you want sensible iconography defaults,
/// use ``DefaultIconographyManeuverInstructionView``,
/// which builds on this with default iconography.
public struct ManeuverInstructionView<ManeuverView: View>: View {
    private let text: String
    private let distanceToNextManeuver: CLLocationDistance?
    private let distanceFormatter: Formatter
    private let maneuverView: ManeuverView
    private let theme: InstructionRowTheme

    /// Initialize a maneuver instruction view that includes a custom leading view or icon..
    /// As an HStack, this view automatically corrects for .rightToLeft languages.
    ///
    /// - Parameters:
    ///   - text: The maneuver instruction.
    ///   - distanceToNextManeuver: The distance to the next step.
    ///   - maneuverView: The custom view representing the maneuver.
    ///   - distanceFormatter: The formatter which controls distance localization.
    ///   - theme: The instruction row theme specifies attributes like colors and fonts for the row.
    public init(
        text: String,
        distanceFormatter: Formatter,
        distanceToNextManeuver: CLLocationDistance? = nil,
        theme: InstructionRowTheme = DefaultInstructionRowTheme(),
        @ViewBuilder maneuverView: () -> ManeuverView = { EmptyView() }
    ) {
        self.text = text
        self.distanceToNextManeuver = distanceToNextManeuver
        self.distanceFormatter = distanceFormatter
        self.maneuverView = maneuverView()
        self.theme = theme
    }

    public var body: some View {
        HStack {
            maneuverView
                .frame(width: 64)
                .foregroundStyle(theme.iconTintColor)

            VStack(alignment: .leading) {
                if let distanceToNextManeuver {
                    Text("\(distanceFormatter.string(for: distanceToNextManeuver) ?? "")")
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
    let arabicFormatter = MKDistanceFormatter()
    arabicFormatter.locale = Locale(identifier: "ar-SA")

    return VStack {
        ManeuverInstructionView(
            text: "Turn Right on Road Ave.",
            distanceFormatter: MKDistanceFormatter(),
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
            distanceFormatter: MKDistanceFormatter(),
            distanceToNextManeuver: 152.4
        ) {
            ManeuverImage(maneuverType: .merge, maneuverModifier: .left)
                .frame(width: 24)
        }
        .font(.body)
        .foregroundColor(.blue)

        ManeuverInstructionView(
            text: "Make a legal u-turn",
            distanceFormatter: MKDistanceFormatter(),
            distanceToNextManeuver: 152.4
        ) {
            ManeuverImage(maneuverType: .turn, maneuverModifier: .uTurn)
                .frame(width: 24)
        }
        .font(.body)
        .foregroundColor(.blue)

        // Demonstrate a Right to Left
        ManeuverInstructionView(
            text: "ادمج يسارًا",
            distanceFormatter: arabicFormatter,
            distanceToNextManeuver: 100
        ) {
            ManeuverImage(maneuverType: .merge, maneuverModifier: .left)
                .frame(width: 24)
        }
        .environment(\.layoutDirection, .rightToLeft)
    }
}
