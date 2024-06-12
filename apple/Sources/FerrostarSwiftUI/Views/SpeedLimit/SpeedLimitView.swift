import SwiftUI

public struct SpeedLimitView: View {
    @Environment(\.locale) private var locale

    var speedLimit: Measurement<UnitSpeed>
    var valueFormatter: NumberFormatter
    var unitFormatter: MeasurementFormatter

    public init(
        speedLimit: Measurement<UnitSpeed>,
        valueFormatter: NumberFormatter = DefaultFormatters.speedFormatter,
        unitFormatter: MeasurementFormatter = DefaultFormatters.speedWithUnitsFormatter
    ) {
        self.speedLimit = speedLimit
        self.valueFormatter = valueFormatter
        self.unitFormatter = unitFormatter
    }

    public var body: some View {
        switch locale.identifier {
        case "en_US":
            USSpeedLimitView(
                speedLimit: speedLimit,
                valueFormatter: valueFormatter,
                unitFormatter: unitFormatter
            )
        default:
            ROWSpeedLimitView(
                speedLimit: speedLimit,
                valueFormatter: valueFormatter,
                unitFormatter: unitFormatter
            )
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
