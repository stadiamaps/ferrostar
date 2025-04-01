import SwiftUI
import TestSupport
import XCTest
@testable import FerrostarMapLibreUI

final class LandscapeNavigationViewTests: XCTestCase {
    // TODO: This needs a fixed reference date for now. See TripProgressViewTests.
    //       The reason we haven't solved this is, it needs to be propagated through
    //       a much larger stack of views in this case.
//    func testDefault() {
//        assertView(frame: CGSize(width: 700, height: 350)) {
//            LandscapeNavigationView(
//                styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
//                camera: .constant(.automotiveNavigation()),
//                navigationState: .pedestrianExample,
//                isMuted: false,
//                onTapMute: {}
//            )
//            .navigationFormatterCollection(TestingFormatterCollection())
//        }
//    }

    func testCustomized() {
        assertView(frame: CGSize(width: 700, height: 350)) {
            LandscapeNavigationView(
                styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
                camera: .constant(.automotiveNavigation()),
                navigationState: .pedestrianExample,
                isMuted: false,
                onTapMute: {}
            )
            .navigationViewProgressView { state, _ in
                Text("Progress: \(state?.currentProgress?.distanceToNextManeuver ?? -1)")
                    .background(Color.blue)
                    .padding()
            }
            .navigationViewInstructionView { state, _, _ in
                Text("Instruction: \(state?.currentVisualInstruction?.primaryContent.text ?? "unknown")")
                    .background(Color.purple)
                    .padding()
            }
            .navigationViewCurrentRoadView { state in
                Text("Current Road: \(state?.currentRoadName ?? "unknown")")
                    .background(Color.yellow)
                    .padding()
            }
            .navigationFormatterCollection(TestingFormatterCollection())
        }
    }
}
