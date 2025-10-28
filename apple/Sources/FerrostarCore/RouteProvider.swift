import CoreLocation
import FerrostarCoreFFI

/// An abstraction around the various ways of getting routes.
public enum RouteProvider {
    /// A provider optimized for a request/response model such as HTTP or socket communications.
    case routeAdapter(RouteAdapterProtocol)
    /// A provider commonly used for local route generation.
    ///
    /// Extensible for any sort of custom route generation that doesn't fit the ``routeAdapter(_:)`` use case.
    case customProvider(CustomRouteProvider)
}

/// A custom route provider is a generic asynchronous route generator.
///
/// The typical use case for a custom route provider is local route generation, but it is generally useful for any route
/// generation that doesn't involve a standardized request generation (ex: HTTP POST) -> request execution
/// (`URLSession`) -> response parsing (stream of bytes) flow.
///
/// This applies well to offline route generation, since you are not getting back a stream of bytes (ex: from a socket)
/// that need decoding, but rather a data structure from a function call which just needs mapping into the Ferrostar
/// route model.
public protocol CustomRouteProvider {
    func getRoutes(userLocation: UserLocation, waypoints: [Waypoint]) async throws -> [Route]
}

public extension WellKnownRouteProvider {
    func withJsonOptions(options: [String: Any]) throws -> WellKnownRouteProvider {
        guard
            let jsonOptions = try String(
                data: JSONSerialization.data(withJSONObject: options),
                encoding: .utf8
            )
        else {
            throw InstantiationError.OptionsJsonParseError
        }

        switch self {
        case .valhalla(endpointUrl: let endpointUrl, profile: let profile, optionsJson: _):
            return .valhalla(endpointUrl: endpointUrl, profile: profile, optionsJson: jsonOptions)
        case .graphHopper(
            endpointUrl: let endpointUrl,
            profile: let profile,
            locale: let locale,
            voiceUnits: let voiceUnits,
            optionsJson: _
        ):
            return .graphHopper(
                endpointUrl: endpointUrl,
                profile: profile,
                locale: locale,
                voiceUnits: voiceUnits,
                optionsJson: jsonOptions
            )
        }
    }
}
