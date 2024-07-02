import XCTest
@testable import FerrostarSwiftUI

final class DefaultFormatterTests: XCTestCase {
    let referenceDate = Date(timeIntervalSince1970: 1_718_065_239)

    // MARK: Distance Formatter

    func testDistanceFormatter() {
        let formatter = DefaultFormatters.distanceFormatter
        formatter.units = .imperial
        XCTAssertEqual(formatter.string(fromDistance: 150), "500 ft")
    }

    func testDistanceFormatter_de_DE() {
        let formatter = DefaultFormatters.distanceFormatter
        formatter.locale = .init(identifier: "de_DE")
        formatter.units = .metric
        XCTAssertEqual(formatter.string(fromDistance: 150), "150 m")
    }

    // MARK: Estimated Time of Arrival (ETA) Formatter

    func testEstimatedArrivalFormatter() {
        var formatter = DefaultFormatters.estimatedArrivalFormat
        formatter.timeZone = .init(secondsFromGMT: 0)!

        XCTAssertEqual(referenceDate.formatted(formatter), "12:20 AM")
    }

    func testEstimatedArrivalFormatter_de_DE() {
        var formatter = DefaultFormatters.estimatedArrivalFormat
            .locale(.init(identifier: "de_DE"))
        formatter.timeZone = .init(secondsFromGMT: 0)!

        XCTAssertEqual(referenceDate.formatted(formatter), "0:20")
    }

    // MARK: Duration Formatters

    func testDurationFormatter() {
        let formatter = DefaultFormatters.durationFormat
        let duration: TimeInterval = 1200.0
        XCTAssertEqual(formatter.string(from: duration), "20m")
    }

    func testDurationFormatter_Long() {
        let formatter = DefaultFormatters.durationFormat
        let duration: TimeInterval = 120_000.0
        XCTAssertEqual(formatter.string(from: duration), "33h 20m")
    }
}
