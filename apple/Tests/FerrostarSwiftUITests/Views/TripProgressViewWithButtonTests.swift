import XCTest
@testable import FerrostarCoreFFI
@testable import FerrostarSwiftUI

final class TripProgressViewWithButtonTests: XCTestCase {
    let formatterCollection = TestingFormatterCollection()

    let referenceDate = Date(timeIntervalSince1970: 1_718_065_239)

    var informationalTheme: any TripProgressViewTheme {
        var theme = DefaultTripProgressViewTheme()
        theme.style = .informational
        return theme
    }

    func testTripProgressViewDefaultTheme() {
        assertView {
            TripProgressView(
                progress: TripProgress(
                    distanceToNextManeuver: 123,
                    distanceRemaining: 120,
                    durationRemaining: 150
                ),
                distanceFormatter: formatterCollection.distanceFormatter,
                estimatedArrivalFormatter: formatterCollection.estimatedArrivalFormatter,
                fromDate: referenceDate,
                onTapExit: {}
            )
        }

        assertView {
            TripProgressView(
                progress: TripProgress(
                    distanceToNextManeuver: 5420,
                    distanceRemaining: 1_420_000,
                    durationRemaining: 520_800
                ),
                distanceFormatter: formatterCollection.distanceFormatter,
                estimatedArrivalFormatter: formatterCollection.estimatedArrivalFormatter,
                fromDate: referenceDate,
                onTapExit: {}
            )
        }
    }

    func testTripProgressViewCompactTheme() {
        assertView {
            TripProgressView(
                progress: TripProgress(
                    distanceToNextManeuver: 5420,
                    distanceRemaining: 1_420_000,
                    durationRemaining: 520_800
                ),
                distanceFormatter: formatterCollection.distanceFormatter,
                estimatedArrivalFormatter: formatterCollection.estimatedArrivalFormatter,
                theme: informationalTheme,
                fromDate: referenceDate,
                onTapExit: {}
            )
        }
    }

    func testTripProgressViewFormatters() {
        assertView {
            TripProgressView(
                progress: TripProgress(
                    distanceToNextManeuver: 5420,
                    distanceRemaining: 1_420_000,
                    durationRemaining: 520_800
                ),
                distanceFormatter: germanDistanceFormatter,
                estimatedArrivalFormatter: formatterCollection.estimatedArrivalFormatter,
                durationFormatter: longDurationFormatter,
                fromDate: referenceDate,
                onTapExit: {}
            )
        }
    }

    func testTripProgressViewFormatters_de_DE() {
        assertView(navigationFormatterCollection: TestingFormatterCollection().locale(Locale(identifier: "de_DE"))) {
            TripProgressView(
                progress: TripProgress(
                    distanceToNextManeuver: 123,
                    distanceRemaining: 14500,
                    durationRemaining: 1234
                ),
                distanceFormatter: germanDistanceFormatter,
                estimatedArrivalFormatter: germanArrivalFormatter,
                fromDate: referenceDate,
                onTapExit: {}
            )
        }

        assertView(navigationFormatterCollection: TestingFormatterCollection().locale(Locale(identifier: "de_DE"))) {
            TripProgressView(
                progress: TripProgress(
                    distanceToNextManeuver: 5420,
                    distanceRemaining: 1_420_000,
                    durationRemaining: 520_800
                ),
                distanceFormatter: germanDistanceFormatter,
                estimatedArrivalFormatter: germanArrivalFormatter,
                theme: informationalTheme,
                fromDate: referenceDate,
                onTapExit: {}
            )
        }
    }

    // MARK: Dark Mode

    func testTripProgressViewDefaultTheme_darkMode() {
        assertView(colorScheme: .dark) {
            TripProgressView(
                progress: TripProgress(
                    distanceToNextManeuver: 123,
                    distanceRemaining: 120,
                    durationRemaining: 150
                ),
                distanceFormatter: formatterCollection.distanceFormatter,
                estimatedArrivalFormatter: formatterCollection.estimatedArrivalFormatter,
                fromDate: referenceDate,
                onTapExit: {}
            )
        }

        assertView(colorScheme: .dark) {
            TripProgressView(
                progress: TripProgress(
                    distanceToNextManeuver: 5420,
                    distanceRemaining: 1_420_000,
                    durationRemaining: 520_800
                ),
                distanceFormatter: formatterCollection.distanceFormatter,
                estimatedArrivalFormatter: formatterCollection.estimatedArrivalFormatter,
                fromDate: referenceDate,
                onTapExit: {}
            )
        }
    }

    func testTripProgressViewCompactTheme_darkMode() {
        assertView(colorScheme: .dark) {
            TripProgressView(
                progress: TripProgress(
                    distanceToNextManeuver: 5420,
                    distanceRemaining: 1_420_000,
                    durationRemaining: 520_800
                ),
                distanceFormatter: formatterCollection.distanceFormatter,
                estimatedArrivalFormatter: formatterCollection.estimatedArrivalFormatter,
                theme: informationalTheme,
                fromDate: referenceDate,
                onTapExit: {}
            )
        }
    }

    func testTripProgressViewFormatters_darkMode() {
        assertView(colorScheme: .dark) {
            TripProgressView(
                progress: TripProgress(
                    distanceToNextManeuver: 5420,
                    distanceRemaining: 1_420_000,
                    durationRemaining: 520_800
                ),
                distanceFormatter: germanDistanceFormatter,
                estimatedArrivalFormatter: formatterCollection.estimatedArrivalFormatter,
                durationFormatter: longDurationFormatter,
                fromDate: referenceDate,
                onTapExit: {}
            )
        }
    }

    func testTripProgressViewFormatters_de_DE_darkMode() {
        assertView(
            colorScheme: .dark,
            navigationFormatterCollection: TestingFormatterCollection().locale(Locale(identifier: "de_DE"))
        ) {
            TripProgressView(
                progress: TripProgress(
                    distanceToNextManeuver: 123,
                    distanceRemaining: 14500,
                    durationRemaining: 1234
                ),
                distanceFormatter: germanDistanceFormatter,
                estimatedArrivalFormatter: germanArrivalFormatter,
                fromDate: referenceDate,
                onTapExit: {}
            )
        }

        assertView(
            colorScheme: .dark,
            navigationFormatterCollection: TestingFormatterCollection().locale(Locale(identifier: "de_DE"))
        ) {
            TripProgressView(
                progress: TripProgress(
                    distanceToNextManeuver: 5420,
                    distanceRemaining: 1_420_000,
                    durationRemaining: 520_800
                ),
                distanceFormatter: germanDistanceFormatter,
                estimatedArrivalFormatter: germanArrivalFormatter,
                theme: informationalTheme,
                fromDate: referenceDate,
                onTapExit: {}
            )
        }
    }
}
