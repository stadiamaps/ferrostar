import MapKit
import SwiftUI

struct LiveActivityView: View {
    let theme: InstructionRowTheme = DefaultInstructionRowTheme()
    let distanceFormatter: Formatter = MKDistanceFormatter()

    let state: TripActivityAttributes.ContentState

    var body: some View {
        HStack(alignment: .center) {
            if let maneuverType = state.instruction.primaryContent.maneuverType {
                ManeuverImage(
                    maneuverType: maneuverType,
                    maneuverModifier: state.instruction.primaryContent.maneuverModifier
                )
                .frame(width: 48)
            }

            Text(state.instruction.primaryContent.text)
                .font(theme.instructionFont)
                .foregroundStyle(theme.instructionColor)
                .fontWeight(.bold)

            if let distance = distanceFormatter.string(for: state.distanceToNextManeuver) {
                Text(verbatim: distance)
                    .font(.title2)
                    .fontWeight(.bold)
                    .padding(.horizontal, 12)
            }
        }
    }
}

#Preview {
    LiveActivityView(
        state: .init(
            instruction: VisualInstructionFactory().build(),
            distanceToNextManeuver: 123
        )
    )
}
