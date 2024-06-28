import SwiftUI

public struct SpeedLimitView: View {
    
    /// The speed limit sign style toggling method. This is used to
    /// define the method for swapping between USStyle & ViennaConvention
    public enum ToggleStyleBy {
        // TODO: Add improved automatic methods (e.g. route admins, etc)
        
        /// Only use the US Style
        case usStyle
        
        /// Only use the Vienna Convention Style
        case viennaConvention
    }

    @Environment(\.locale) private var locale

    var toggleStyleBy: ToggleStyleBy
    var speedLimit: Measurement<UnitSpeed>
    var valueFormatter: NumberFormatter
    var unitFormatter: MeasurementFormatter

    public init(
        speedLimit: Measurement<UnitSpeed>,
        toggleStyleBy: ToggleStyleBy = .viennaConvention, // Change the default once we have a better solution.
        valueFormatter: NumberFormatter = DefaultFormatters.speedFormatter,
        unitFormatter: MeasurementFormatter = DefaultFormatters.speedWithUnitsFormatter
    ) {
        self.toggleStyleBy = toggleStyleBy
        self.speedLimit = speedLimit
        self.valueFormatter = valueFormatter
        self.unitFormatter = unitFormatter
    }

    public var body: some View {
        if useUSStyle() {
            USStyleSpeedLimitView(
                speedLimit: speedLimit,
                valueFormatter: valueFormatter,
                unitFormatter: unitFormatter
            )
        } else {
            ViennaConventionStyleSpeedLimitView(
                speedLimit: speedLimit,
                valueFormatter: valueFormatter,
                unitFormatter: unitFormatter
            )
        }
    }
    
    private func useUSStyle() -> Bool {
        switch toggleStyleBy {
        case .usStyle:
            return SpeedLimitFixedToUSStyle().useUSStyle()
        case .viennaConvention:
            return SpeedLimitFixedToViennaConventionStyle().useUSStyle()
        }
    }
}

#Preview {
    VStack {
        SpeedLimitView(speedLimit: .init(value: 24.5, unit: .metersPerSecond))

        SpeedLimitView(speedLimit: .init(value: 27.8, unit: .metersPerSecond))
            .environment(\.locale, .init(identifier: "fr_FR"))
    }
    .padding()
    .background(Color.green)
}
