import SwiftUI
import XCTest
@testable import FerrostarCore
@testable import FerrostarSwiftUI

final class NavigatingInnerGridViewTests: XCTestCase {
    func test_USStyle_speedLimit_inGridView() {
        assertView {
            NavigatingInnerGridView(
                speedLimit: .init(value: 55, unit: .milesPerHour),
                speedLimitStyle: .mutcdStyle,
                isMuted: true,
                onMute: {},
                showZoom: true,
                cameraControlState: .showRecenter {}
            )
            .padding()
        }
    }

    func test_ViennaConventionStyle_speedLimit_inGridView() {
        assertView {
            NavigatingInnerGridView(
                speedLimit: .init(value: 100, unit: .kilometersPerHour),
                speedLimitStyle: .viennaConvention,
                isMuted: false,
                onMute: {},
                showZoom: true,
                cameraControlState: .showRecenter {}
            )
            .environment(\.locale, .init(identifier: "fr_FR"))
            .padding()
        }
    }

    func test_muteIsHidden() {
        assertView {
            NavigatingInnerGridView(
                isMuted: true,
                showMute: false,
                onMute: {},
                showZoom: true,
                cameraControlState: .showRecenter {}
            )
            .environment(\.locale, .init(identifier: "fr_FR"))
            .padding()
        }
    }

    func test_CameraControlsHidden() {
        assertView {
            NavigatingInnerGridView(
                isMuted: true,
                showMute: true,
                onMute: {},
                showZoom: true,
                cameraControlState: .hidden
            )
            .padding()
        }
    }

    func test_RouteOverviewCameraControl() {
        assertView {
            NavigatingInnerGridView(
                isMuted: true,
                showMute: true,
                onMute: {},
                showZoom: true,
                cameraControlState: .showRouteOverview {}
            )
            .padding()
        }
    }
}
