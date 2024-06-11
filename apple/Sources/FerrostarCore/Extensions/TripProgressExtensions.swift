import FerrostarCoreFFI
import Foundation

public extension TripProgress {
    /// The estimated arrival date time. This is typically formatted into a time for the
    /// arrival view.
    func estimatedArrival(from startingDate: Date = Date()) -> Date {
        startingDate.addingTimeInterval(durationRemaining)
    }
}
