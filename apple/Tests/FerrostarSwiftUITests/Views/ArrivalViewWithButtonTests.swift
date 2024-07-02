import XCTest
@testable import FerrostarCoreFFI
@testable import FerrostarSwiftUI

final class ArrivalViewWithButtonTests: XCTestCase {
    let formatterCollection = TestingFormatterCollection()

    let referenceDate = Date(timeIntervalSince1970: 1_718_065_239)

    var informationalTheme: any ArrivalViewTheme {
        var theme = DefaultArrivalViewTheme()
        theme.style = .informational
        return theme
    }

    func testArrivalViewDefaultTheme() {
        assertView {
            ArrivalView(
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
            ArrivalView(
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

    func testArrivalViewCompactTheme() {
        assertView {
            ArrivalView(
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

    func testArrivalViewFormatters() {
        assertView {
            ArrivalView(
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

    func testArrivalViewFormatters_de_DE() {
        assertView(navigationFormatterCollection: TestingFormatterCollection().locale(Locale(identifier: "de_DE"))) {
            ArrivalView(
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
            ArrivalView(
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

    func testArrivalViewDefaultTheme_darkMode() {
        assertView(colorScheme: .dark) {
            ArrivalView(
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
            ArrivalView(
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

    func testArrivalViewCompactTheme_darkMode() {
        assertView(colorScheme: .dark) {
            ArrivalView(
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

    func testArrivalViewFormatters_darkMode() {
        assertView(colorScheme: .dark) {
            ArrivalView(
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

    func testArrivalViewFormatters_de_DE_darkMode() {
        assertView(
            colorScheme: .dark,
            navigationFormatterCollection: TestingFormatterCollection().locale(Locale(identifier: "de_DE"))
        ) {
            ArrivalView(
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
            ArrivalView(
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
