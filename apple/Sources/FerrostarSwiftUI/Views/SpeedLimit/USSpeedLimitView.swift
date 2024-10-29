import SwiftUI

/// A US-style speed limit sign using a rectangular white box and a black border.
public struct USStyleSpeedLimitView: View {
    var speedLimit: Measurement<UnitSpeed>
    var units: UnitSpeed
    var valueFormatter: NumberFormatter
    var unitFormatter: MeasurementFormatter

    public init(
        speedLimit: Measurement<UnitSpeed>,
        units: UnitSpeed = .milesPerHour,
        valueFormatter: NumberFormatter = DefaultFormatters.speedFormatter,
        unitFormatter: MeasurementFormatter = DefaultFormatters.speedWithUnitsFormatter
    ) {
        self.speedLimit = speedLimit.converted(to: units)
        self.units = units
        self.valueFormatter = valueFormatter
        self.unitFormatter = unitFormatter
    }

    public var body: some View {
        ZStack(alignment: .center) {
            RoundedRectangle(cornerRadius: 8)
                .foregroundColor(.white)
                .frame(width: 60, height: 84)

            RoundedRectangle(cornerRadius: 6)
                .foregroundColor(.black)
                .frame(width: 56, height: 80)

            RoundedRectangle(cornerRadius: 4)
                .foregroundColor(.white)
                .frame(width: 52, height: 76)

            VStack {
                Text("Speed Limit", bundle: .module)
                    .font(.caption.bold())
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .padding(.horizontal, 2)

                Text(valueFormatter.string(from: speedLimit.value as NSNumber) ?? "")
                    .font(.title2.bold())
                    .minimumScaleFactor(0.4)
                    .padding(.horizontal, 2)

                Text(speedLimit.unit.symbol)
                    .font(.caption2.bold())
                    .foregroundStyle(Color.secondary)
                    .padding(.horizontal, 2)
            }
            .foregroundColor(.black)
            .background(Color.white)
            .frame(width: 52, height: 76)
            .cornerRadius(4)
            .colorScheme(.light)
        }
    }
}

#Preview {
    VStack {
        USStyleSpeedLimitView(speedLimit: .init(value: 50, unit: .milesPerHour))

        USStyleSpeedLimitView(speedLimit: .init(value: 100, unit: .milesPerHour))

        USStyleSpeedLimitView(speedLimit: .init(value: 10000, unit: .milesPerHour))

        USStyleSpeedLimitView(speedLimit: .init(value: 50, unit: .milesPerHour),
                              units: .kilometersPerHour)
    }
    .padding()
    .background(Color.green)
}
