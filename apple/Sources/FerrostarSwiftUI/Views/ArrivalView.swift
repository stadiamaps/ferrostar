import FerrostarCore
import FerrostarCoreFFI
import MapKit
import SwiftUI

public struct ArrivalView: View {
    let progress: TripProgress
    let distanceFormatter: Formatter
    let estimatedArrivalFormatter: Date.FormatStyle
    let durationFromatter: DateComponentsFormatter
    let theme: any ArrivalViewTheme
    let fromDate: Date
    
    /// Initialize the ArrivalView
    ///
    /// - Parameters:
    ///   - progress: The current Trip Progress providing durations and distances.
    ///   - distanceFormatter: The distance formatter to use when displaying the remaining trip distance.
    ///   - estimatedArrivalFormatter: The estimated time of arrival Date-Time formatter.
    ///   - durationFormatter: The duration remaining formatter.
    ///   - theme: The arrival view theme.
    ///   - fromDate: The date time to estimate arrival from, primarily for testing (default is now).
    public init(
        progress: TripProgress,
        distanceFormatter: Formatter = ArrivalFormatters.distanceFormatter,
        estimatedArrivalFormatter: Date.FormatStyle = ArrivalFormatters.estimatedArrivalFormat,
        durationFormatter: DateComponentsFormatter = ArrivalFormatters.durationFormat,
        theme: any ArrivalViewTheme = DefaultArrivalViewTheme(),
        fromDate: Date = Date()
    ) {
        self.progress = progress
        self.distanceFormatter = distanceFormatter
        self.estimatedArrivalFormatter = estimatedArrivalFormatter
        durationFromatter = durationFormatter
        self.theme = theme
        self.fromDate = fromDate
    }

    public var body: some View {
        HStack {
            VStack {
                Text(estimatedArrivalFormatter.format(progress.estimatedArrival(from: fromDate)))
                    .font(theme.measurementFont)
                    .foregroundStyle(theme.measurementColor)
                    .multilineTextAlignment(.center)

                if theme.style == .full {
                    Text("Arrival", bundle: .module)
                        .font(theme.secondaryFont)
                        .foregroundStyle(theme.secondaryColor)
                }
            }

            if let formattedDuration = durationFromatter.string(from: progress.durationRemaining) {
                VStack {
                    Text(formattedDuration)
                        .font(theme.measurementFont)
                        .foregroundStyle(theme.measurementColor)
                        .frame(maxWidth: .infinity)
                        .multilineTextAlignment(.center)

                    if theme.style == .full {
                        Text("Duration", bundle: .module)
                            .font(theme.secondaryFont)
                            .foregroundStyle(theme.secondaryColor)
                    }
                }
            }

            VStack {
                Text(distanceFormatter.string(for: progress.distanceRemaining) ?? "")
                    .font(theme.measurementFont)
                    .foregroundStyle(theme.measurementColor)
                    .multilineTextAlignment(.center)

                if theme.style == .full {
                    Text("Distance", bundle: .module)
                        .font(theme.secondaryFont)
                        .foregroundStyle(theme.secondaryColor)
                }
            }
        }
        .padding(.horizontal, 32)
        .padding(.vertical, 16)
        .background(theme.backgroundColor)
        .clipShape(.rect(cornerRadius: 48))
        .shadow(radius: 12)
    }
}

#Preview {
    var minimizedTheme: any ArrivalViewTheme {
        var theme = DefaultArrivalViewTheme()
        theme.style = .minimized
        return theme
    }

    return VStack(spacing: 16) {
        ArrivalView(
            progress: TripProgress(
                distanceToNextManeuver: 123,
                distanceRemaining: 120,
                durationRemaining: 150
            )
        )

        ArrivalView(
            progress: TripProgress(
                distanceToNextManeuver: 123,
                distanceRemaining: 14500,
                durationRemaining: 1234
            )
        )

        ArrivalView(
            progress: TripProgress(
                distanceToNextManeuver: 5420,
                distanceRemaining: 1_420_000,
                durationRemaining: 520_800
            )
        )

        ArrivalView(
            progress: TripProgress(
                distanceToNextManeuver: 5420,
                distanceRemaining: 1_420_000,
                durationRemaining: 520_800
            ),
            theme: minimizedTheme
        )

        Spacer()
    }
    .padding()
    .background(Color.green)
}
