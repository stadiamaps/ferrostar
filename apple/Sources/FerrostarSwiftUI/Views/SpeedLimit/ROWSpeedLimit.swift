import SwiftUI

/// The common speed circular limit sign with a white background and red border.
/// Also known as the Vienna Convention Sign C14.
public struct ViennaConventionStyleSpeedLimitView: View {
    var speedLimit: Measurement<UnitSpeed>
    var units: UnitSpeed
    var valueFormatter: NumberFormatter
    var unitFormatter: MeasurementFormatter

    public init(
        speedLimit: Measurement<UnitSpeed>,
        units: UnitSpeed = .kilometersPerHour,
        valueFormatter: NumberFormatter = DefaultFormatters.speedFormatter,
        unitFormatter: MeasurementFormatter = DefaultFormatters.speedWithUnitsFormatter
    ) {
        self.speedLimit = speedLimit.converted(to: units)
        self.units = units
        self.valueFormatter = valueFormatter
        self.unitFormatter = unitFormatter
    }

    public var body: some View {
        ZStack {
            Circle()
                .foregroundStyle(Color.red)

            Circle()
                .foregroundStyle(Color.white)
                .frame(width: 56, height: 56)

            VStack {
                Text(valueFormatter.string(from: speedLimit.value as NSNumber) ?? "")
                    .font(.title2.bold())
                    .minimumScaleFactor(0.4)
                    .padding(.horizontal, 6)

                Text(unitFormatter.string(from: speedLimit.unit))
                    .font(.caption2.bold())
                    .foregroundStyle(Color.secondary)
                    .minimumScaleFactor(0.4)
                    .padding(.horizontal, 6)
            }
            .frame(width: 56, height: 56)
        }
        .frame(width: 64, height: 64)
        .colorScheme(.light)
    }
}

#Preview {
    VStack {
        ViennaConventionStyleSpeedLimitView(speedLimit: .init(value: 50, unit: .kilometersPerHour))

        ViennaConventionStyleSpeedLimitView(speedLimit: .init(value: 100, unit: .kilometersPerHour))

        ViennaConventionStyleSpeedLimitView(speedLimit: .init(value: 1000, unit: .kilometersPerHour))

        ViennaConventionStyleSpeedLimitView(
            speedLimit: .init(value: 100, unit: .kilometersPerHour),
            units: .milesPerHour
        )
    }
    .padding()
    .background(Color.green)
}
