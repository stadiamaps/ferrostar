import CoreLocation
import StadiaMaps
import StadiaMapsAutocompleteSearch
import SwiftUI

struct SearchSheet: View {
    let userLocation: CLLocation?
    let onTapDestination: (Point) -> Void

    var body: some View {
        VStack(spacing: 0) {
            AutocompleteSearch(
                apiKey: sharedAPIKeys.stadiaMapsAPIKey,
                userLocation: userLocation
            ) {
                guard let geometry = $0.geometry else { return }
                onTapDestination(geometry)
            }
        }
        .background(.white)
    }
}

#Preview {
    SearchSheet(
        userLocation: .init(),
        onTapDestination: { _ in }
    )
}
