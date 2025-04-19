import FerrostarCore
import FerrostarCoreFFI
import FerrostarSwiftUI
import SwiftUI

struct InstructionsViewWrapper: View {
    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection

    let navigationState: NavigationState?
    let isExpanded: Binding<Bool>
    let sizeWhenNotExpanded: Binding<CGSize>

    var body: some View {
        if case .navigating = navigationState?.tripState,
           let visualInstruction = navigationState?.currentVisualInstruction,
           let progress = navigationState?.currentProgress,
           let remainingSteps = navigationState?.remainingSteps
        {
            InstructionsView(
                visualInstruction: visualInstruction,
                distanceFormatter: formatterCollection.distanceFormatter,
                distanceToNextManeuver: progress.distanceToNextManeuver,
                remainingSteps: remainingSteps,
                isExpanded: isExpanded,
                sizeWhenNotExpanded: sizeWhenNotExpanded
            )
        }
    }
}
