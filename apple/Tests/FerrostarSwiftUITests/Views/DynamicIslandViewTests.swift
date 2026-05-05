import MapKit
import SwiftUI
import TestSupport
import XCTest
@testable import FerrostarCore
@testable import FerrostarCoreFFI
@testable import FerrostarSwiftUI

final class DynamicIslandViewTests: XCTestCase {
    /// A locale-pinned distance formatter for deterministic snapshot output.
    private let testDistanceFormatter: MKDistanceFormatter = {
        let formatter = MKDistanceFormatter()
        formatter.locale = Locale(identifier: "en_US")
        formatter.units = .imperial
        return formatter
    }()

    func testLiveActivityManeuverImage() {
        assertView {
            dynamicIslandWrapper {
                LiveActivityManeuverImage(
                    state: .init(
                        instruction: VisualInstructionFactory().build(),
                        distanceToNextManeuver: 123
                    )
                )
            }
            .padding()
        }
    }

    func testLiveActivityView() {
        assertView {
            dynamicIslandWrapper {
                LiveActivityView(
                    distanceFormatter: testDistanceFormatter,
                    state: .init(
                        instruction: VisualInstructionFactory().build(),
                        distanceToNextManeuver: 123
                    )
                )
            }
            .padding()
        }
    }

    func testLiveActivityMinimalView() {
        assertView {
            minimalDynamicIslandWrapper {
                LiveActivityMinimalView(
                    state: .init(
                        instruction: VisualInstructionFactory().build(),
                        distanceToNextManeuver: 123
                    ),
                    distanceFormatter: testDistanceFormatter
                )
            }
            .padding()
        }
    }

    private func dynamicIslandWrapper(
        @ViewBuilder body: () -> some View
    ) -> some View {
        body()
            .foregroundColor(.white)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                RoundedRectangle(cornerRadius: 20)
                    .fill(.black)
                    .shadow(color: .black.opacity(0.3), radius: 4, x: 0, y: 2)
            )
            .frame(width: 371, height: 48)
    }

    private func minimalDynamicIslandWrapper(
        @ViewBuilder body: () -> some View
    ) -> some View {
        body()
            .foregroundColor(.white)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(
                RoundedRectangle(cornerRadius: 18)
                    .fill(.black)
                    .shadow(color: .black.opacity(0.3), radius: 3, x: 0, y: 1)
            )
            .frame(width: 126, height: 28)
    }
}
