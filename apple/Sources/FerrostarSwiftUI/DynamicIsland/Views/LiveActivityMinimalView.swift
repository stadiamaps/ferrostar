import MapKit
import SwiftUI

struct LiveActivityMinimalView: View {
    let state: TripActivityAttributes.ContentState
    let distanceFormatter: Formatter

    var body: some View {
        VStack(alignment: .center, spacing: 2) {
            LiveActivityManeuverImage(state: state)

            if let distance = distanceFormatter.string(for: state.distanceToNextManeuver) {
                Text(verbatim: distance)
                    .font(.caption)
                    .minimumScaleFactor(0.1)
                    .frame(height: 6)
            }
        }
        .padding(.horizontal, 8)
    }
}

#Preview {
    LiveActivityMinimalView(
        state: .init(
            instruction: VisualInstructionFactory().build(),
            distanceToNextManeuver: 123
        ),
        distanceFormatter: MKDistanceFormatter()
    )
}
