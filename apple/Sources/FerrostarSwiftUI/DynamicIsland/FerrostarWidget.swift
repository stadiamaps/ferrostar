import ActivityKit
import CoreLocation
import FerrostarCoreFFI
import MapKit
import SwiftUI
import WidgetKit

@available(iOS 16.2, *)
public struct FerrostarWidget: Widget {
    var theme: InstructionRowTheme = DefaultInstructionRowTheme()

    var distanceFormatter: Formatter = {
        let formatter = MKDistanceFormatter()
        formatter.unitStyle = .abbreviated
        return formatter
    }()

    public init() {}

    public var body: some WidgetConfiguration {
        ActivityConfiguration(for: TripActivityAttributes.self) { context in
            LiveActivityView(state: context.state)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading, priority: 1) {
                    HStack(alignment: .center) {
                        LiveActivityManeuverImage(state: context.state)
                    }
                    .frame(width: 48)
                    .frame(maxHeight: .infinity)
                    .padding(.horizontal)
                }

                DynamicIslandExpandedRegion(.center) {
                    Text(context.state.instruction.primaryContent.text)
                        .font(theme.instructionFont)
                        .foregroundStyle(theme.instructionColor)
                        .fontWeight(.bold)
                }

                DynamicIslandExpandedRegion(.trailing, priority: 1) {
                    HStack(alignment: .center) {
                        if let distance = distanceFormatter.string(for: context.state.distanceToNextManeuver) {
                            Text(verbatim: distance)
                                .font(.title2)
                                .fontWeight(.bold)
                                .padding(.horizontal, 12)
                        }
                    }
                    .frame(maxHeight: .infinity)
                }

                // TODO: End Navigation if Arrived
//                DynamicIslandExpandedRegion(.bottom) {
//                    InstructionsView(
//                        visualInstruction: context.state.instruction
//                    )
//                }
            } compactLeading: {
                LiveActivityManeuverImage(state: context.state)
            } compactTrailing: {
                if let distance = distanceFormatter.string(for: context.state.distanceToNextManeuver) {
                    Text(verbatim: distance)
                }
            } minimal: {
                VStack {
                    LiveActivityManeuverImage(state: context.state)

                    if let distance = distanceFormatter.string(for: context.state.distanceToNextManeuver) {
                        Text(verbatim: distance)
                    }
                }
            }
            .keylineTint(.blue)
        }
    }
}
