import Foundation
import SwiftUI

// MARK: - Speed Limit Configuration Environment

public struct SpeedLimitConfiguration {
    public let speedLimit: Measurement<UnitSpeed>?
    public let speedLimitStyle: SpeedLimitView.SignageStyle

    public init(speedLimit: Measurement<UnitSpeed>? = nil, speedLimitStyle: SpeedLimitView.SignageStyle = .mutcdStyle) {
        self.speedLimit = speedLimit
        self.speedLimitStyle = speedLimitStyle
    }

    // MARK: - Convenience Accessors

    /// Get the speed limit value, if available.
    public var getSpeedLimit: Measurement<UnitSpeed>? {
        speedLimit
    }

    /// Get the speed limit style.
    public var getSpeedLimitStyle: SpeedLimitView.SignageStyle {
        speedLimitStyle
    }
}

private struct SpeedLimitConfigurationKey: EnvironmentKey {
    static var defaultValue: SpeedLimitConfiguration = .init()
}

public extension EnvironmentValues {
    var speedLimitConfiguration: SpeedLimitConfiguration {
        get { self[SpeedLimitConfigurationKey.self] }
        set { self[SpeedLimitConfigurationKey.self] = newValue }
    }
}

// MARK: - Speed Limit View Modifier

private struct SpeedLimitViewModifier: ViewModifier {
    let speedLimit: Measurement<UnitSpeed>?
    let speedLimitStyle: SpeedLimitView.SignageStyle

    func body(content: Content) -> some View {
        content
            .transformEnvironment(\.speedLimitConfiguration) { config in
                // Merge new configuration with existing, prioritizing new values
                config = SpeedLimitConfiguration(
                    speedLimit: speedLimit ?? config.speedLimit,
                    speedLimitStyle: speedLimitStyle
                )
            }
    }
}

// MARK: - Type-Safe Extensions

/// Protocol for views that can display speed limit information
public protocol SpeedLimitViewHost where Self: View {
    // No stored properties - views should read from environment instead
}

public extension View {
    /// Configure the view hierarchy to display a speed limit with a specific signage style.
    ///
    /// This modifier sets speed limit information in the environment that can be read
    /// by child views that conform to SpeedLimitViewHost.
    ///
    /// - Parameters:
    ///   - speedLimit: The current speed limit in the desired units to display.
    ///   - speedLimitStyle: The style of the signage (US-MUTCD or Vienna Convention).
    /// - Returns: A modified view with speed limit configuration in the environment.
    func navigationSpeedLimit(
        speedLimit: Measurement<UnitSpeed>?,
        speedLimitStyle: SpeedLimitView.SignageStyle
    ) -> some View {
        modifier(SpeedLimitViewModifier(
            speedLimit: speedLimit,
            speedLimitStyle: speedLimitStyle
        ))
    }
}
