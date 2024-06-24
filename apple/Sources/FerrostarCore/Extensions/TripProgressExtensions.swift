import FerrostarCoreFFI
import Foundation

public extension TripProgress {
    /// The estimated arrival date and time.
    func estimatedArrival(from startingDate: Date = Date()) -> Date {
        startingDate.addingTimeInterval(durationRemaining)
    }
}
