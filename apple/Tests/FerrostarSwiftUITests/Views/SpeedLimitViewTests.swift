import XCTest
@testable import FerrostarSwiftUI

final class SpeedLimitViewTests: XCTestCase {
    func testUSStyleSpeedLimitViews() {
        assertView {
            USStyleSpeedLimitView(speedLimit: .init(value: 50, unit: .milesPerHour))
        }

        assertView {
            USStyleSpeedLimitView(speedLimit: .init(value: 100, unit: .milesPerHour))
        }

        assertView {
            USStyleSpeedLimitView(speedLimit: .init(value: 10000, unit: .milesPerHour))
        }

        assertView {
            USStyleSpeedLimitView(speedLimit: .init(value: 50, unit: .milesPerHour),
                                  units: .kilometersPerHour)
        }
    }

    func testViennaConventionStyleSpeedLimitViews() {
        assertView {
            ViennaConventionStyleSpeedLimitView(speedLimit: .init(value: 50, unit: .kilometersPerHour))
        }

        assertView {
            ViennaConventionStyleSpeedLimitView(speedLimit: .init(value: 100, unit: .kilometersPerHour))
        }

        assertView {
            ViennaConventionStyleSpeedLimitView(speedLimit: .init(value: 1000, unit: .kilometersPerHour))
        }

        assertView {
            ViennaConventionStyleSpeedLimitView(
                speedLimit: .init(value: 100, unit: .kilometersPerHour),
                units: .milesPerHour
            )
        }
    }

    func assertLocalizedSpeedLimitViews() {
        assertView {
            SpeedLimitView(speedLimit: .init(value: 24.5, unit: .metersPerSecond))
        }

        assertView {
            SpeedLimitView(speedLimit: .init(value: 27.8, unit: .metersPerSecond))
                .environment(\.locale, .init(identifier: "fr_FR"))
        }
    }
}
