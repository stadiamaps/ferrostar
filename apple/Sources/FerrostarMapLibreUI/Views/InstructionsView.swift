import SwiftUI
import FerrostarCoreFFI

struct InstructionsView: View {
    
    private let visualInstruction: VisualInstruction
    
    init(visualInstruction: VisualInstruction) {
        self.visualInstruction = visualInstruction
    }
    
    var body: some View {
        VStack {
            DefaultManeuverInstructionView(
                text: visualInstruction.primaryContent.text,
                maneuverType: visualInstruction.primaryContent.maneuverType,
                maneuverModifier: .left
            )
            .font(.title2.bold())
            .padding(.horizontal, 16)
            .padding(.top, 16)
            
            if let secondaryContent = visualInstruction.secondaryContent {
                VStack {
                    DefaultManeuverInstructionView(
                        text: secondaryContent.text,
                        maneuverType: secondaryContent.maneuverType,
                        maneuverModifier: secondaryContent.maneuverModifier
                    )
                    .frame(height: 24)
                    .font(.title3)
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    
                    RoundedRectangle(cornerRadius: 3)
                        .frame(width: 24, height: 6)
                        .opacity(0.1)
                        .padding(.bottom, 8)
                }
                .background(.gray.opacity(0.2))
            }
            
            
        }
        .background(Color.white)
        .clipShape(.rect(cornerRadius: 12))
        .padding()
        .shadow(radius: 12)
    }
}

// TODO: Re-enable
//#Preview {
//    VStack {
//        InstructionsView(
//            visualInstruction: VisualInstruction(
//                primaryContent: <#T##VisualInstructionContent#>,
//                secondaryContent: <#T##VisualInstructionContent?#>,
//                triggerDistanceBeforeManeuver: <#T##Double#>
//            )
//        )
//        
//        Spacer()
//    }
//    .background(Color.green)
//}
