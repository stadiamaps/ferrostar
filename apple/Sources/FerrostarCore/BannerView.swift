
import CoreLocation
import FerrostarCoreFFI
import MapKit
import SwiftUI

extension VisualInstructionContent {
    // Stand-in art using SFSymbols for now. Ideally look for some
    // iconography licensed under CC or similar that we can use on all
    // platforms.
    var maneuverIcon: String? {
        guard let type = maneuverType else {
            return nil
        }

        if type == .merge {
            return "arrow.triangle.merge"
        }

        // TODO: Probably special cases for depart, arrive, and fork?

        guard let modifier = maneuverModifier else {
            return nil
        }

        switch modifier {
        case .uTurn:
            return "arrow.uturn.down"
        case .sharpRight:
            return "arrow.turn.up.right"
        case .right:
            return "arrow.turn.up.right"
        case .slightRight:
            return "arrow.turn.up.right"
        case .straight:
            return "arrow.up"
        case .slightLeft:
            return "arrow.turn.up.left"
        case .left:
            return "arrow.turn.up.left"
        case .sharpLeft:
            return "arrow.turn.up.left"
        }
    }
}

public struct BannerView: View {
    let instructions: VisualInstruction
    let distanceToNextManeuver: CLLocationDistance?
    let formatter = MKDistanceFormatter()

    public init(instructions: VisualInstruction, distanceToNextManeuver: CLLocationDistance?) {
        self.instructions = instructions
        self.distanceToNextManeuver = distanceToNextManeuver
    }

    public var body: some View {
        VStack {
            HStack {
                VStack {
                    if let maneuverIcon = instructions.primaryContent.maneuverIcon {
                        Image(systemName: maneuverIcon)
                            .font(.title)
                            .foregroundColor(.white)
                    }
                    if let distanceToNextManeuver {
                        Text("\(formatter.string(fromDistance: distanceToNextManeuver))")
                            .foregroundStyle(.white)
                    }
                }
                Text(instructions.primaryContent.text)
                    .font(.largeTitle)
                    .foregroundStyle(.white)
            }

            if let secondary = instructions.secondaryContent {
                Text(secondary.text)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.all, 8)
        .background(Color.black.opacity(0.7).clipShape(.rect(cornerRadius: 10), style: FillStyle()))
        .padding(.horizontal, 16)
    }
}

#Preview {
    let instructions = VisualInstruction(
        primaryContent: VisualInstructionContent(text: "Hyde Street", maneuverType: .turn, maneuverModifier: .left,
                                                 roundaboutExitDegrees: nil),
        secondaryContent: nil,
        triggerDistanceBeforeManeuver: 42.0
    )

    return BannerView(instructions: instructions, distanceToNextManeuver: 42)
}
