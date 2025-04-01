import SwiftUI
import TestSupport
import XCTest
@testable import FerrostarMapLibreUI

final class DynamicallyOrientingViewTests: XCTestCase {
    func testDefault() {
        assertView {
            DynamicallyOrientingNavigationView(
                styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
                camera: .constant(.automotiveNavigation()),
                navigationState: .pedestrianExample,
                isMuted: false,
                onTapMute: {}
            )
        }
    }

    func testCustomized() {
        assertView {
            DynamicallyOrientingNavigationView(
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
        }
    }
}
