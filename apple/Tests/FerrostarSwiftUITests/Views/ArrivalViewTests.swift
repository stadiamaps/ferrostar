import XCTest
@testable import FerrostarCoreFFI
@testable import FerrostarSwiftUI

final class ArrivalViewTests: XCTestCase {
    let referenceDate = Date(timeIntervalSince1970: 1_718_065_239)

    var etaFormatter: Date.FormatStyle = .init(timeZone: .init(secondsFromGMT: 0)!)
        .hour(.defaultDigits(amPM: .abbreviated))
        .minute(.twoDigits)

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
                estimatedArrivalFormatter: etaFormatter,
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
                estimatedArrivalFormatter: etaFormatter,
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
                estimatedArrivalFormatter: etaFormatter,
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
                estimatedArrivalFormatter: etaFormatter,
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
                estimatedArrivalFormatter: etaFormatter,
                durationFormatter: longDurationFormatter,
                fromDate: referenceDate
            )
        }
    }
}
