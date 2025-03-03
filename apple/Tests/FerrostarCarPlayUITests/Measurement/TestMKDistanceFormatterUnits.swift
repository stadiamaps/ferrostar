import MapKit
import Testing

@testable import FerrostarCarPlayUI

struct TestMKDistanceFormatterUnits {
    @Test("test metric units", arguments: [
        Locale(identifier: "fr_FR"),
        Locale(identifier: "de_DE"),
        Locale(identifier: "ja_JP"),
    ])
    func testMetricUnits(locale: Locale) throws {
        let (shortUnit, longUnit) = MKDistanceFormatter.Units.metric.getShortAndLong(
            for: locale
        )
        #expect(shortUnit == .meters)
        #expect(longUnit == .kilometers)
    }

    @Test("Test imperial units", arguments: [
        Locale(identifier: "en_US"),
        Locale(identifier: "en_CA"),
    ])
    func testImperialUnits(locale: Locale) throws {
        let (shortUnit, longUnit) = MKDistanceFormatter.Units.imperial.getShortAndLong(
            for: locale
        )
        #expect(shortUnit == .feet)
        #expect(longUnit == .miles)
    }

    @Test("Test imperial UK", arguments: [
        Locale(identifier: "en_GB"),
    ])
    func testImperialWithYards(locale: Locale) throws {
        let (shortUnit, longUnit) = MKDistanceFormatter.Units.imperialWithYards.getShortAndLong(
            for: locale
        )
        #expect(shortUnit == .yards)
        #expect(longUnit == .miles)
    }

    // MARK: Test Thresholds

    @Test("test metric threshold", arguments: [
        Locale(identifier: "fr_FR"),
        Locale(identifier: "de_DE"),
        Locale(identifier: "ja_JP"),
    ])
    func testMetricThreshold(locale: Locale) throws {
        #expect(MKDistanceFormatter.Units.metric.thresholdForLargeUnit(for: locale) == 1000)
    }

    @Test("Test imperial threshold", arguments: [
        Locale(identifier: "en_US"),
        Locale(identifier: "en_CA"),
    ])
    func testImperialUnitsThreshold(locale: Locale) throws {
        #expect(MKDistanceFormatter.Units.imperial.thresholdForLargeUnit(for: locale) == 289)
    }

    @Test("Test imperial UK threshold", arguments: [
        Locale(identifier: "en_GB"),
    ])
    func testImperialUKThreshold(locale _: Locale) throws {
        #expect(
            MKDistanceFormatter.Units.imperialWithYards.thresholdForLargeUnit(for: .current) == 300
        )
    }
}
