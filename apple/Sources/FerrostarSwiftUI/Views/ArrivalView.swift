import FerrostarCore
import FerrostarCoreFFI
import MapKit
import SwiftUI

public struct ArrivalView: View {
    let progress: TripProgress
    let distanceFormatter: Formatter
    let estimatedArrivalFormatter: Date.FormatStyle
    let durationFromatter: DateComponentsFormatter
    let theme: ArrivalViewTheme
    let onExpand: () -> Void

    public init(
        progress: TripProgress,
        distanceFormatter: Formatter = ArrivalFormatters.distanceFormatter,
        estimatedArrivalFormatter: Date.FormatStyle = ArrivalFormatters.estimatedArrivalFormat,
        durationFormatter: DateComponentsFormatter = ArrivalFormatters.durationFormat,
        theme: ArrivalViewTheme = DefaultArrivalViewTheme(),
        onExpand: @escaping () -> Void = {}
    ) {
        self.progress = progress
        self.distanceFormatter = distanceFormatter
        self.estimatedArrivalFormatter = estimatedArrivalFormatter
        durationFromatter = durationFormatter
        self.theme = theme
        self.onExpand = onExpand
    }

    public var body: some View {
        HStack {
            VStack {
                Text(estimatedArrivalFormatter.format(progress.estimatedArrival()))
                    .font(theme.measurementFont)
                    .foregroundStyle(theme.measurementColor)
                    .multilineTextAlignment(.center)

                if theme.style == .full {
                    Text("Arrival")
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
                        Text("Duration")
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
                    Text("Distance")
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
    var minimizedTheme: ArrivalViewTheme {
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
