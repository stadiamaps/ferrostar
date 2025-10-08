import CarPlay
import CoreLocation
import Foundation
import os
import StadiaMaps
import StadiaMapsAutocompleteSearchAPI

private extension Logger {
    static let issue = Logger(subsystem: "com.stadiamaps.search-carplay", category: "issue")
}

private let FeaturePropertiesKey = "com.stadiamaps.search.featureproperties"

private extension CPListItem {
    var featureProperties: FeaturePropertiesV2? {
        get {
            guard let info = userInfo as? [String: Any] else { return nil }
            return info[FeaturePropertiesKey] as? FeaturePropertiesV2
        }
        set {
            userInfo = [FeaturePropertiesKey: newValue]
        }
    }
}

private extension FeaturePropertiesV2Properties {
    var listItem: CPListItem {
        CPListItem(text: name, detailText: coarseLocation, image: UIImage(systemName: systemName))
    }
}

private extension FeaturePropertiesV2 {
    var listItem: CPListItem {
        let item = properties.listItem
        item.featureProperties = self
        return item
    }
}

private enum CarPlaySearchError: Error {
    case noHandler
    case noFeatureProperties
    case notListItem
}

final class DemoCarPlaySearch: NSObject, CPSearchTemplateDelegate {
    private let apiKey: String
    private let useEUEndpoint: Bool
    private let interfaceController: CPInterfaceController
    private var userLocation: CLLocation?
    private let limitLayers: [LayerId]?
    private let minSearchLength: Int
    private var onResultSelected: ((FeaturePropertiesV2) throws -> Void)?
    private var onError: ((Error) -> Void)?

    private var searchText = ""
    private var searchResults: [CPListItem] = []
    private var previousTemplate: CPTemplate?

    init(apiKey: String,
         useEUEndpoint: Bool = false,
         interfaceController: CPInterfaceController,
         limitLayers: [LayerId]? = nil,
         minSearchLength: Int = 1)
    {
        self.apiKey = apiKey
        self.useEUEndpoint = useEUEndpoint
        self.interfaceController = interfaceController
        self.limitLayers = limitLayers
        self.minSearchLength = minSearchLength
    }

    func searchNear(
        userLocation: CLLocation?,
        onResultSelected: @escaping ((FeaturePropertiesV2) throws -> Void),
        onError: @escaping ((Error) -> Void)
    ) {
        self.userLocation = userLocation
        self.onResultSelected = onResultSelected
        self.onError = onError

        let template = CPSearchTemplate()
        template.delegate = self

        previousTemplate = interfaceController.topTemplate

        var pushError: Error?
        interfaceController.pushTemplate(template, animated: true) { _, error in
            pushError = error
        }
        if let pushError { handleError(pushError) }
    }

    func searchTemplate(_: CPSearchTemplate, updatedSearchText searchText: String) async -> [CPListItem] {
        do {
            if self.searchText != searchText {
                self.searchText = searchText
                try await search(query: searchText, autocomplete: false)
            }
        } catch {
            handleError(error)
        }
        return searchResults
    }

    func searchTemplate(_: CPSearchTemplate, selectedResult item: CPListItem) async {
        do {
            try handleItem(item)
        } catch {
            handleError(error)
        }
    }

    func searchTemplateSearchButtonPressed(_: CPSearchTemplate) {
        do {
            let listTemplate = CPListTemplate(
                title: "\"\(searchText)\"",
                sections: [CPListSection(items: searchResults)]
            )
            var pushError: Error?
            interfaceController.pushTemplate(listTemplate, animated: true) { _, error in
                pushError = error
            }
            if let pushError { throw pushError }
        } catch {
            handleError(error)
        }
    }

    private func handleError(_ error: Error) {
        Logger.issue.error("\(error.localizedDescription)")

        guard let onError else { return }
        onError(error)
    }

    private func handleItem(_ item: CPListItem) throws {
        guard let onResultSelected else {
            throw CarPlaySearchError.noHandler
        }
        guard let featureProperties = item.featureProperties else {
            throw CarPlaySearchError.noFeatureProperties
        }

        var popError: Error?
        if let previousTemplate {
            interfaceController.pop(to: previousTemplate, animated: true) { _, error in
                popError = error
            }
        } else {
            interfaceController.popTemplate(animated: true) { _, error in
                popError = error
            }
        }
        if let popError { throw popError }

        try onResultSelected(featureProperties)
    }

    private func itemHandler(_ selectableListItem: any CPSelectableListItem, completion: () -> Void) {
        do {
            guard let featureItem = selectableListItem as? CPListItem
            else { throw CarPlaySearchError.notListItem }

            try handleItem(featureItem)
        } catch {
            handleError(error)
        }
        completion()
    }

    private func search(query: String, autocomplete: Bool) async throws {
        let features = try await GeocodingAPI.autocompletingSearch(
            query: query,
            autocomplete: autocomplete,
            apiKey: apiKey,
            useEUEndpoint: useEUEndpoint,
            userLocation: userLocation,
            minSearchLength: minSearchLength,
            limitLayers: limitLayers
        )

        // Only replace results if the text matches the current input
        if query == searchText {
            let items = features.map(\.listItem)
            for item in items {
                item.handler = itemHandler(_:completion:)
            }

            searchResults = items
        }
    }
}
