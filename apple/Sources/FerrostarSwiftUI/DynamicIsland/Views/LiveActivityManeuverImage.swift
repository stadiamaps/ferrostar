import SwiftUI

struct LiveActivityManeuverImage: View {
    let state: TripActivityAttributes.ContentState

    var body: some View {
        if let maneuverType = state.instruction.primaryContent.maneuverType {
            ManeuverImage(
                maneuverType: maneuverType,
                maneuverModifier: state.instruction.primaryContent.maneuverModifier
            )
        } else {
            Image(systemName: "location.north.line.fill")
        }
    }
}

#Preview {
    LiveActivityManeuverImage(
        state: .init(
            instruction: VisualInstructionFactory().build(),
            distanceToNextManeuver: 123
        )
    )
}
