import CarPlay
import FerrostarCoreFFI
import MapKit
import Testing

@testable import FerrostarCarPlayUI

struct CPTravelEstimatesTests {
    @Test("Initialize TripProgress for Trip")
    func testFromTrip() async throws {
        let tripProgress = TripProgress(
            distanceToNextManeuver: 11.1,
            distanceRemaining: 22.2,
            durationRemaining: 33.3
        )

        let estimates = CPTravelEstimates.fromFerrostarForTrip(
            progress: tripProgress,
            units: .imperial,
            locale: .init(identifier: "en_US")
        )

        #expect(estimates.distanceRemaining == .init(value: 70, unit: .feet))
        #expect(estimates.timeRemaining == 33.3)
    }

    @Test("Initialize TripProgress for Step")
    func testFromStep() async throws {
        let tripProgress = TripProgress(
            distanceToNextManeuver: 11.1,
            distanceRemaining: 22.2,
            durationRemaining: 33.3
        )

        let estimates = CPTravelEstimates.fromFerrostarForStep(
            progress: tripProgress,
            units: .metric,
            locale: .init(identifier: "en_US")
        )

        #expect(estimates.distanceRemaining == .init(value: 10.0, unit: .meters))
        #expect(estimates.timeRemaining == 0)
    }
}
