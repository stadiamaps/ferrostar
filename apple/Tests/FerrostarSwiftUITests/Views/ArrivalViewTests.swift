import XCTest
@testable import FerrostarCoreFFI
@testable import FerrostarSwiftUI

final class ArrivalViewTests: XCTestCase {
    let referenceDate = Date(timeIntervalSince1970: 1718065239)
    
    var minimizedTheme: any ArrivalViewTheme {
        var theme = DefaultArrivalViewTheme()
        theme.style = .minimized
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
                fromDate: referenceDate
            )
        }

        assertView {
            ArrivalView(
                progress: TripProgress(
                    distanceToNextManeuver: 123,
                    distanceRemaining: 14500,
                    durationRemaining: 1234
                ),
                fromDate: referenceDate
            )
        }

        assertView {
            ArrivalView(
                progress: TripProgress(
                    distanceToNextManeuver: 5420,
                    distanceRemaining: 1_420_000,
                    durationRemaining: 520_800
                ),
                fromDate: referenceDate
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
                theme: minimizedTheme,
                fromDate: referenceDate
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
                estimatedArrivalFormatter: .dateTime,
                durationFormatter: longDurationFormatter,
                fromDate: referenceDate
            )
        }
    }
}
