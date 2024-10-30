import SwiftUI

public struct SpeedLimitView: View {
    /// The speed limit sign style toggling method. This is used to
    /// define the method for swapping between USStyle & ViennaConvention
    public enum SignageStyle {
        // TODO: Add improved automatic methods (e.g. route admins, etc)

        /// Only use the MUTCD style (mostly in the US)
        case mutcdStyle

        /// Only use the Vienna Convention Style
        case viennaConvention
    }

    @Environment(\.locale) private var locale

    var signageStyle: SignageStyle
    var speedLimit: Measurement<UnitSpeed>
    var valueFormatter: NumberFormatter
    var unitFormatter: MeasurementFormatter

    public init(
        speedLimit: Measurement<UnitSpeed>,
        signageStyle: SignageStyle = .viennaConvention, // Change the default once we have a better solution.
        valueFormatter: NumberFormatter = DefaultFormatters.speedFormatter,
        unitFormatter: MeasurementFormatter = DefaultFormatters.speedWithUnitsFormatter
    ) {
        self.signageStyle = signageStyle
        self.speedLimit = speedLimit
        self.valueFormatter = valueFormatter
        self.unitFormatter = unitFormatter
    }

    public var body: some View {
        if useMUTCDStyle() {
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

    private func useMUTCDStyle() -> Bool {
        switch signageStyle {
        case .mutcdStyle:
            FixedMUTCDStyle().useMUTCDStyle()
        case .viennaConvention:
            FixedViennaConventionStyle().useMUTCDStyle()
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
