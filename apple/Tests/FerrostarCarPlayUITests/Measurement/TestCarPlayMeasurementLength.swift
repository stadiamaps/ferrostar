import MapKit
import Testing

@testable import FerrostarCarPlayUI

struct TestCarPlayMeasurementLength {
    // MARK: Short Distance Rounding Tests (feet/meters/yards)

    @Test(
        "Test rounding under 50 units",
        arguments: [
            (12.3, 10.0),
            (17.8, 20.0),
            (42.4, 40.0),
            (47.6, 50.0),
        ]
    )
    func testRoundingUnder50(testCase: (input: Double, expected: Double)) throws {
        let measurement = CarPlayMeasurementLength(
            units: .imperial,
            distance: testCase.input * 0.3048 // Convert feet to meters
        )
        let rounded = measurement.rounded()
        #expect(rounded.value == testCase.expected)
        #expect(rounded.unit == .feet)
    }

    @Test(
        "Test rounding between 50 and 100 units",
        arguments: [
            (54.4, 50.0),
            (65.6, 70.0),
            (88.3, 90.0),
            (95.6, 100.0),
        ]
    )
    func testRoundingBetween50And100(testCase: (input: Double, expected: Double)) throws {
        let measurement = CarPlayMeasurementLength(
            units: .imperial,
            distance: testCase.input * 0.3048
        )
        let rounded = measurement.rounded()
        #expect(rounded.value == testCase.expected)
        #expect(rounded.unit == .feet)
    }

    @Test(
        "Test rounding between 100 and 500 units",
        arguments: [
            (124.0, 100.0),
            (275.0, 300.0),
            (442.0, 450.0),
            (476.0, 500.0),
        ]
    )
    func testRoundingBetween100And500(testCase: (input: Double, expected: Double)) throws {
        let measurement = CarPlayMeasurementLength(
            units: .imperial,
            distance: testCase.input * 0.3048
        )
        let rounded = measurement.rounded()
        #expect(rounded.value == testCase.expected)
        #expect(rounded.unit == .feet)
    }

    @Test(
        "Test rounding above 500 units",
        arguments: [
            (524.0, 500.0),
            (649.0, 600.0),
            (751.0, 800.0),
        ]
    )
    func testRoundingAbove500(testCase: (input: Double, expected: Double)) throws {
        let measurement = CarPlayMeasurementLength(
            units: .imperial,
            distance: testCase.input * 0.3048
        )
        let rounded = measurement.rounded()
        #expect(rounded.value == testCase.expected)
        #expect(rounded.unit == .feet)
    }

    // MARK: Long Distance Rounding Tests (kilometers/miles)

    @Test(
        "Test rounding kilometers under 10",
        arguments: [
            (1.24, 1.2),
            (3.56, 3.6),
            (5.74, 5.7),
            (9.95, 10.0),
        ]
    )
    func testRoundingKilometersUnder10(testCase: (input: Double, expected: Double)) throws {
        let measurement = CarPlayMeasurementLength(
            units: .metric,
            distance: testCase.input * 1000 // Convert km to meters
        )
        let rounded = measurement.rounded()
        #expect(rounded.value == testCase.expected)
        #expect(rounded.unit == .kilometers)
    }

    @Test(
        "Test rounding kilometers above 10",
        arguments: [
            (10.4, 10.0),
            (12.6, 13.0),
            (15.3, 15.0),
            (19.7, 20.0),
        ]
    )
    func testRoundingKilometersAbove10(testCase: (input: Double, expected: Double)) throws {
        let measurement = CarPlayMeasurementLength(
            units: .metric,
            distance: testCase.input * 1000
        )
        let rounded = measurement.rounded()
        #expect(rounded.value == testCase.expected)
        #expect(rounded.unit == .kilometers)
    }

    @Test(
        "Test rounding miles under 10",
        arguments: [
            (1.24, 1.2),
            (3.56, 3.6),
            (5.74, 5.7),
            (9.95, 9.9),
        ]
    )
    func testRoundingMilesUnder10(testCase: (input: Double, expected: Double)) throws {
        let measurement = CarPlayMeasurementLength(
            units: .imperial,
            distance: testCase.input * 1609.34 // Convert miles to meters
        )
        let rounded = measurement.rounded()
        #expect(rounded.value == testCase.expected)
        #expect(rounded.unit == .miles)
    }

    @Test(
        "Test rounding miles above 10",
        arguments: [
            (10.4, 10.0),
            (12.6, 13.0),
            (15.3, 15.0),
            (19.7, 20.0),
        ]
    )
    func testRoundingMilesAbove10(testCase: (input: Double, expected: Double)) throws {
        let measurement = CarPlayMeasurementLength(
            units: .imperial,
            distance: testCase.input * 1609.34
        )
        let rounded = measurement.rounded()
        #expect(rounded.value == testCase.expected)
        #expect(rounded.unit == .miles)
    }
}
