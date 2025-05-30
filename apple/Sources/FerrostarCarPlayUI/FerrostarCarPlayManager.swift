import CarPlay
import FerrostarCore
import Foundation
import MapLibreSwiftUI
import os
import OSLog
import SwiftUI

private extension Logger {
    static let cpMapTemplateDelegate = Logger(category: "CPMapTemplateDelegate")
}

public class FerrostarCarPlayManager: NSObject {
    private let logger: Logger

    private var ferrostarAdapter: FerrostarCarPlayAdapter

    public var mapTemplate: CPMapTemplate = .init()

    public init(
        _ ferrostarCore: FerrostarCore,
        logger: Logger = Logger(
            subsystem: Bundle.main.bundleIdentifier ?? "FerrostarCarPlayUI",
            category: "FerrostarCarPlayManager"
        ),
        distanceUnits: MKDistanceFormatter.Units,
        showCentering: Bool,
        onCenter: @escaping () -> Void,
        onStartTrip: @escaping () -> Void,
        onCancelTrip: @escaping () -> Void
    ) {
        self.logger = logger

        // Create the navigation adapter
        ferrostarAdapter = FerrostarCarPlayAdapter(ferrostarCore: ferrostarCore,
                                                   distanceUnits: distanceUnits,
                                                   mapTemplate: mapTemplate,
                                                   showCentering: showCentering,
                                                   onCenter: onCenter,
                                                   onStartTrip: onStartTrip,
                                                   onCancelTrip: onCancelTrip)

        super.init()
    }

    public func disconnect() {
        logger.debug("\(#function)")
    }
}
