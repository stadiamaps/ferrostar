import ActivityKit
import CoreLocation
import FerrostarCore
import FerrostarCoreFFI
import OSLog

private let logger = Logger(subsystem: "com.stadiamaps.ferrostar", category: "FerrostarWidgetProvider")

@available(iOS 16.2, *)
public class FerrostarWidgetProvider: WidgetProviding {
    private var activity: Activity<TripActivityAttributes>?
    private var lastUpdateDistance: CLLocationDistance?

    public init() {
        // No def
    }

    public func update(visualInstruction: VisualInstruction, tripProgress: TripProgress) {
        let currentDistance = tripProgress.distanceToNextManeuver

        // Check if we should update based on distance threshold
        if shouldUpdate(currentDistance: currentDistance) {
            lastUpdateDistance = currentDistance

            Task {
                do {
                    try await requestOrUpdate(visualInstruction: visualInstruction, tripProgress: tripProgress)
                } catch {
                    logger.error("Failed to update Dynamic Island activity: \(error.localizedDescription)")
                }
            }
        }
    }

    public func terminate() async {
        await activity?.end()
        lastUpdateDistance = nil
    }

    private func requestOrUpdate(
        visualInstruction: VisualInstruction,
        tripProgress: TripProgress
    ) async throws {
        let newState = TripActivityAttributes.ContentState(
            instruction: visualInstruction,
            distanceToNextManeuver: tripProgress.distanceToNextManeuver
        )

        guard let activity else {
            activity = try Activity.request(
                attributes: .init(),
                content: .init(state: newState, staleDate: nil)
            )
            return
        }

        // The activity already exists.
        await activity.update(using: newState)
    }

    private func shouldUpdate(currentDistance: CLLocationDistance) -> Bool {
        guard let lastDistance = lastUpdateDistance else {
            // First update
            return true
        }

        let distanceChange = abs(lastDistance - currentDistance)
        let threshold = updateThreshold(for: currentDistance)

        return distanceChange >= threshold
    }

    private func updateThreshold(for distance: CLLocationDistance) -> CLLocationDistance {
        // TODO: This could be way nicer, but it get's the job done as a starting point.
        // Progressive scaling: closer to maneuver = more frequent updates
        switch distance {
        case 0 ..< 50: // < 50m: update every 5m
            5
        case 50 ..< 100: // 50-100m: update every 10m
            10
        case 100 ..< 200: // 100-200m: update every 15m
            15
        case 200 ..< 500: // 200-500m: update every 25m
            25
        case 500 ..< 1000: // 500m-1km: update every 50m
            50
        default: // > 1km: update every 100m
            100
        }
    }
}
