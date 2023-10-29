
import SwiftUI
import UniFFI
import CoreLocation
import MapKit

extension UniFFI.VisualInstructionContent {
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

struct BannerView: View {
    let instructions: VisualInstructions
    let distanceToNextManeuver: CLLocationDistance?
    let formatter = MKDistanceFormatter()

    var body: some View {
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
        .background(Color.black.opacity(0.7).clipShape(.buttonBorder, style: FillStyle()))
        .padding(.horizontal, 16)
    }
}

#Preview {
    let location = GeographicCoordinates(lng: 0, lat: 0)
    let instructions = UniFFI.VisualInstructions(primaryContent: VisualInstructionContent(text: "Hyde Street", maneuverType: .turn, maneuverModifier: .left, roundaboutExitDegrees: nil), secondaryContent: nil, triggerDistanceBeforeManeuver: 42.0)

    return BannerView(instructions: instructions, distanceToNextManeuver: 42)
}
