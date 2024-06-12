import XCTest
@testable import FerrostarSwiftUI

final class SpeedLimitViewTests: XCTestCase {

    func testUSSpeedLimitViews() {
        assertView {
            USSpeedLimitView(speedLimit: .init(value: 50, unit: .milesPerHour))
        }
            
        assertView {
            USSpeedLimitView(speedLimit: .init(value: 100, unit: .milesPerHour))
        }
            
        assertView {
            USSpeedLimitView(speedLimit: .init(value: 10000, unit: .milesPerHour))
        }
            
        assertView {
            USSpeedLimitView(speedLimit: .init(value: 50, unit: .milesPerHour),
                             units: .kilometersPerHour)
        }
    }
    
    func testROWSpeedLimitViews() {
        assertView {
            ROWSpeedLimitView(speedLimit: .init(value: 50, unit: .kilometersPerHour))
        }
            
        assertView {
            ROWSpeedLimitView(speedLimit: .init(value: 100, unit: .kilometersPerHour))
        }
            
        assertView {
            ROWSpeedLimitView(speedLimit: .init(value: 1000, unit: .kilometersPerHour))
        }
            
        assertView {
            ROWSpeedLimitView(speedLimit: .init(value: 100, unit: .kilometersPerHour), units: .milesPerHour)
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
