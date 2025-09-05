import CarPlay
import OSLog
@preconcurrency import StadiaMaps
import StadiaMapsAutocompleteSearchAPI
import SwiftUI

private let logger = Logger(subsystem: "DemoApp", category: "DemoCarPlaySearchModel")

@MainActor
@Observable
class DemoCarPlaySearchModel: NSObject {
    @ObservationIgnored
    var searchTemplate: CPSearchTemplate?

    @ObservationIgnored
    weak var navController: DemoCarPlayNavController?

    func onAppear(_ navController: DemoCarPlayNavController) async throws {
        self.navController = navController

        let searchTemplate = CPSearchTemplate()
        searchTemplate.delegate = self
        self.searchTemplate = searchTemplate

        try await CarPlaySession.shared.pushTemplate(searchTemplate)
        updateTemplate()
    }

    func updateTemplate() {
        searchTemplate?.tabImage = UIImage(systemName: "chevron.left")
        searchTemplate?.showsTabBadge = true
    }
}

extension DemoCarPlaySearchModel: @preconcurrency CPSearchTemplateDelegate {
    func searchTemplate(_: CPSearchTemplate, updatedSearchText searchText: String) async -> [CPListItem] {
        do {
            let places = try await GeocodingAPI.autocompletingSearch(
                query: searchText,
                autocomplete: true,
                apiKey: sharedAPIKeys.stadiaMapsAPIKey,
                minSearchLength: 3
            )
            .map { place in
                CPListItem(
                    text: place.properties.name,
                    detailText: place.properties.coarseLocation
                )
            }
            return places
        } catch {
            return []
        }
    }

    func searchTemplate(_: CPSearchTemplate, selectedResult _: CPListItem) async {
        // TODO: Fixme
    }
}
