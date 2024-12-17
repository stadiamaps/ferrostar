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
            SpeedLimitView(speedLimit: .init(value: 24.5, unit: .metersPerSecond), signageStyle: .mutcdStyle)
        }

        assertView {
            SpeedLimitView(speedLimit: .init(value: 27.8, unit: .metersPerSecond), signageStyle: .viennaConvention)
                .environment(\.locale, .init(identifier: "fr_FR"))
        }
    }

    // MARK: Dark Mode

    func testUSStyleSpeedLimitViews_darkMode() {
        assertView(colorScheme: .dark) {
            USStyleSpeedLimitView(speedLimit: .init(value: 50, unit: .milesPerHour))
        }

        assertView(colorScheme: .dark) {
            USStyleSpeedLimitView(speedLimit: .init(value: 100, unit: .milesPerHour))
        }

        assertView(colorScheme: .dark) {
            USStyleSpeedLimitView(speedLimit: .init(value: 10000, unit: .milesPerHour))
        }

        assertView(colorScheme: .dark) {
            USStyleSpeedLimitView(speedLimit: .init(value: 50, unit: .milesPerHour),
                                  units: .kilometersPerHour)
        }
    }

    func testViennaConventionStyleSpeedLimitViews_darkMode() {
        assertView(colorScheme: .dark) {
            ViennaConventionStyleSpeedLimitView(speedLimit: .init(value: 50, unit: .kilometersPerHour))
        }

        assertView(colorScheme: .dark) {
            ViennaConventionStyleSpeedLimitView(speedLimit: .init(value: 100, unit: .kilometersPerHour))
        }

        assertView(colorScheme: .dark) {
            ViennaConventionStyleSpeedLimitView(speedLimit: .init(value: 1000, unit: .kilometersPerHour))
        }

        assertView(colorScheme: .dark) {
            ViennaConventionStyleSpeedLimitView(
                speedLimit: .init(value: 100, unit: .kilometersPerHour),
                units: .milesPerHour
            )
        }
    }

    func assertLocalizedSpeedLimitViews_darkMode() {
        assertView(colorScheme: .dark) {
            SpeedLimitView(speedLimit: .init(value: 24.5, unit: .metersPerSecond), signageStyle: .mutcdStyle)
        }

        assertView(colorScheme: .dark) {
            SpeedLimitView(speedLimit: .init(value: 27.8, unit: .metersPerSecond), signageStyle: .viennaConvention)
                .environment(\.locale, .init(identifier: "fr_FR"))
        }
    }
}
