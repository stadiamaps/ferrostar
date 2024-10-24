import FerrostarCore
import FerrostarCoreFFI
import MapKit
import SwiftUI

public struct TripProgressView: View {
    let progress: TripProgress
    let distanceFormatter: Formatter
    let estimatedArrivalFormatter: Date.FormatStyle
    let durationFormatter: DateComponentsFormatter
    let theme: any TripProgressViewTheme
    let fromDate: Date
    let onTapExit: (() -> Void)?

    /// Creates a view that shows progress throughout the trip.
    ///
    /// - Parameters:
    ///   - progress: The current Trip Progress providing durations and distances.
    ///   - distanceFormatter: The distance formatter to use when displaying the remaining trip distance.
    ///   - estimatedArrivalFormatter: The estimated time of arrival Date-Time formatter.
    ///   - durationFormatter: The duration remaining formatter.
    ///   - theme: The trip progress view theme.
    ///   - fromDate: The date time to estimate arrival from, primarily for testing (default is now).
    ///   - onTapExit: The action to run when the exit button is tapped.
    public init(
        progress: TripProgress,
        distanceFormatter: Formatter = DefaultFormatters.distanceFormatter,
        estimatedArrivalFormatter: Date.FormatStyle = DefaultFormatters.estimatedArrivalFormat,
        durationFormatter: DateComponentsFormatter = DefaultFormatters.durationFormat,
        theme: any TripProgressViewTheme = DefaultTripProgressViewTheme(),
        fromDate: Date = Date(),
        onTapExit: (() -> Void)? = nil
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
                    .minimumScaleFactor(0.6)
                    .lineLimit(1)
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
                        .minimumScaleFactor(0.6)
                        .lineLimit(1)
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
                    .minimumScaleFactor(0.6)
                    .lineLimit(1)
                    .foregroundStyle(theme.measurementColor)
                    .multilineTextAlignment(.center)

                if theme.style == .informational {
                    Text("Distance", bundle: .module)
                        .font(theme.secondaryFont)
                        .foregroundStyle(theme.secondaryColor)
                }
            }

            if let onTapExit {
                Button {
                    onTapExit()
                } label: {
                    Image(systemName: "xmark")
                        .foregroundColor(theme.measurementColor)
                }
                .padding()
                .background(Color(.secondarySystemBackground))
                .clipShape(Capsule())
            } else {
                Rectangle()
                    .frame(width: 20, height: 10)
                    .foregroundColor(.clear)
            }
        }
        .padding(.leading, 32)
        .padding(.trailing, 12)
        .padding(.vertical, 8)
        .background(theme.backgroundColor)
        .clipShape(.rect(cornerRadius: 48))
        .shadow(radius: 12)
    }
}

#Preview {
    var informationalTheme: any TripProgressViewTheme {
        var theme = DefaultTripProgressViewTheme()
        theme.style = .informational
        return theme
    }

    return VStack(spacing: 16) {
        TripProgressView(
            progress: TripProgress(
                distanceToNextManeuver: 123,
                distanceRemaining: 120,
                durationRemaining: 150
            )
        )

        TripProgressView(
            progress: TripProgress(
                distanceToNextManeuver: 123,
                distanceRemaining: 14500,
                durationRemaining: 1234
            )
        )

        TripProgressView(
            progress: TripProgress(
                distanceToNextManeuver: 123,
                distanceRemaining: 14500,
                durationRemaining: 12234
            ),
            theme: informationalTheme
        )
        .environment(\.locale, .init(identifier: "de_DE"))

        TripProgressView(
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

#Preview("TripProgressView With Action") {
    var informationalTheme: any TripProgressViewTheme {
        var theme = DefaultTripProgressViewTheme()
        theme.style = .informational
        return theme
    }

    return VStack(spacing: 16) {
        TripProgressView(
            progress: TripProgress(
                distanceToNextManeuver: 123,
                distanceRemaining: 120,
                durationRemaining: 150
            ),
            onTapExit: {}
        )

        TripProgressView(
            progress: TripProgress(
                distanceToNextManeuver: 123,
                distanceRemaining: 14500,
                durationRemaining: 1234
            ),
            onTapExit: {}
        )

        TripProgressView(
            progress: TripProgress(
                distanceToNextManeuver: 123,
                distanceRemaining: 14500,
                durationRemaining: 12234
            ),
            theme: informationalTheme,
            onTapExit: {}
        )
        .environment(\.locale, .init(identifier: "de_DE"))

        TripProgressView(
            progress: TripProgress(
                distanceToNextManeuver: 5420,
                distanceRemaining: 1_420_000,
                durationRemaining: 520_800
            ),
            theme: informationalTheme,
            onTapExit: {}
        )

        Spacer()
    }
    .padding()
    .background(Color.green)
}
