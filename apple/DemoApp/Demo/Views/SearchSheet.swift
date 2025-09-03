import CoreLocation
import StadiaMaps
import StadiaMapsAutocompleteSearch
import SwiftUI

struct SearchSheet: View {
    let userLocation: CLLocation?
    let onTapDestination: (Point) -> Void

    var body: some View {
        VStack(spacing: 0) {
            RoundedRectangle(cornerRadius: 3)
                .frame(width: 24, height: 6)
                .foregroundStyle(.secondary)

            AutocompleteSearch(
                apiKey: sharedAPIKeys.stadiaMapsAPIKey,
                userLocation: userLocation
            ) {
                guard let geometry = $0.geometry else { return }
                onTapDestination(geometry)
            }
        }
        .background(.white)
        .padding(.top)
    }
}

#Preview {
    SearchSheet(
        userLocation: .init(),
        onTapDestination: { _ in }
    )
}
