import SwiftUI
import FerrostarCoreFFI

/// A Customizable Maneuver Instruction View.
public struct ManeuverInstructionView<ManeuverView: View>: View {
        
    private let text: String
    private let distanceRemaining: String?
    private let maneuverView: ManeuverView
    
    /// Initialize a manuever instruction view that includes a custom leading view or icon..
    /// As an HStack, this view automatically corrects for .rightToLeft languages.
    ///
    /// - Parameters:
    ///   - text: The maneuver instruction.
    ///   - distanceRemaining: A string that should represent the localized distance remaining.
    ///   - maneuverView: The custom view representing the maneuver.
    public init(
        text: String,
        distanceRemaining: String? = nil,
        @ViewBuilder maneuverView: () -> ManeuverView = { EmptyView() }
    ) {
        self.text = text
        self.distanceRemaining = distanceRemaining
        self.maneuverView = maneuverView()
    }
    
    public var body: some View {
        HStack {
            maneuverView
                .frame(width: 64)
            
            VStack(alignment: .leading) {
                if let distanceRemaining {
                    Text(distanceRemaining)
                        .font(.title.bold())
                }
                
                Text(text)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                    .font(.title2)
            }
            
            Spacer(minLength: 0)
        }
    }
}

#Preview {
    VStack {
        ManeuverInstructionView(
            text: "Turn Right on Road Ave.",
            distanceRemaining: "15 mi"
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
            distanceRemaining: "500 ft"
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
