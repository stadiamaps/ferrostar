import FerrostarCore
import FerrostarCoreFFI
import MapKit
import SwiftUI

public struct ArrivalView: View {
    let progress: TripProgress
    let distanceFormatter: Formatter
    let estimatedArrivalFormatter: Date.FormatStyle
    let durationFormatter: DateComponentsFormatter
    let theme: any ArrivalViewTheme
    let fromDate: Date
    let onTapExit: () -> Void

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
        distanceFormatter: Formatter = DefaultFormatters.distanceFormatter,
        estimatedArrivalFormatter: Date.FormatStyle = DefaultFormatters.estimatedArrivalFormat,
        durationFormatter: DateComponentsFormatter = DefaultFormatters.durationFormat,
        theme: any ArrivalViewTheme = DefaultArrivalViewTheme(),
        fromDate: Date = Date(),
        onTapExit: @escaping () -> Void = {}
    ) {
        self.progress = progress
        self.distanceFormatter = distanceFormatter
        self.estimatedArrivalFormatter = estimatedArrivalFormatter
        self.durationFormatter = durationFormatter
        self.theme = theme
        self.fromDate = fromDate
        self.onTapExit = onTapExit
    }

    public var body: some View {
        HStack {
            VStack {
                Text(estimatedArrivalFormatter.format(progress.estimatedArrival(from: fromDate)))
                    .font(theme.measurementFont)
                    .foregroundStyle(theme.measurementColor)
                    .multilineTextAlignment(.center)

                if theme.style == .informational {
                    Text("Arrival", bundle: .module)
                        .font(theme.secondaryFont)
                        .foregroundStyle(theme.secondaryColor)
                }
            }

            if let formattedDuration = durationFormatter.string(from: progress.durationRemaining) {
                VStack {
                    Text(formattedDuration)
                        .font(theme.measurementFont)
                        .foregroundStyle(theme.measurementColor)
                        .frame(maxWidth: .infinity)
                        .multilineTextAlignment(.center)

                    if theme.style == .informational {
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

                if theme.style == .informational {
                    Text("Distance", bundle: .module)
                        .font(theme.secondaryFont)
                        .foregroundStyle(theme.secondaryColor)
                }
            }

            Button {
                onTapExit()
            } label: {
                Image(systemName: "x.mark")
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
    var informationalTheme: any ArrivalViewTheme {
        var theme = DefaultArrivalViewTheme()
        theme.style = .informational
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
                distanceToNextManeuver: 123,
                distanceRemaining: 14500,
                durationRemaining: 1234
            ),
            theme: informationalTheme
        )
        .environment(\.locale, .init(identifier: "de_DE"))

        ArrivalView(
            progress: TripProgress(
                distanceToNextManeuver: 5420,
                distanceRemaining: 1_420_000,
                durationRemaining: 520_800
            ),
            theme: informationalTheme
        )

        Spacer()
    }
    .padding()
    .background(Color.green)
}
