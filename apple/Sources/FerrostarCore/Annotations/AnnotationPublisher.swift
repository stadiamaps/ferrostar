import Combine
import Foundation

/// A generic implementation of the annotation publisher.
/// To allow dynamic specialization in the core ``FerrostarCore/FerrostarCore/init(routeProvider:locationProvider:navigationControllerConfig:networkSession:annotation:)``
public protocol AnnotationPublishing {
    associatedtype Annotation: Decodable

    var value: Annotation? { get }
    var speedLimit: Measurement<UnitSpeed>? { get }

    func configure(_ navigationState: Published<NavigationState?>.Publisher)
}

/// A class that publishes the decoded annotation object off of ``FerrostarCore``'s
/// ``NavigationState`` publisher.
public class AnnotationPublisher<Annotation: Decodable>: ObservableObject, AnnotationPublishing {
    @Published public var value: Annotation?
    @Published public var speedLimit: Measurement<UnitSpeed>?

    private let mapSpeedLimit: ((Annotation?) -> Measurement<UnitSpeed>?)?
    private let decoder: JSONDecoder
    private let onError: (Error) -> Void
    private var cancellables = Set<AnyCancellable>()

    /// Create a new annotation publisher with an instance of ``FerrostarCore``
    ///
    /// - Parameters:
    ///   - mapSpeedLimit: Extract and convert the annotation types speed limit (if one exists).
    ///   - onError: A closure to run any time a `DecoderError` occurs.
    ///   - decoder: Specify a custom JSONDecoder if desired.
    public init(
        mapSpeedLimit: ((Annotation?) -> Measurement<UnitSpeed>?)? = nil,
        onError: @escaping (Error) -> Void = { _ in },
        decoder: JSONDecoder = JSONDecoder()
    ) {
        self.mapSpeedLimit = mapSpeedLimit
        self.onError = onError
        self.decoder = decoder
    }

    /// Configure the AnnotationPublisher to run off of a specific navigation state published value.
    ///
    /// - Parameter navigationState: Ferrostar's current navigation state.
    public func configure(_ navigationState: Published<NavigationState?>.Publisher) {
        navigationState
            .map(decodeAnnotation)
            .receive(on: DispatchQueue.main)
            .assign(to: &$value)

        if let mapSpeedLimit {
            $value
                .map(mapSpeedLimit)
                .assign(to: &$speedLimit)
        }
    }

    func decodeAnnotation(_ state: NavigationState?) -> Annotation? {
        guard let data = state?.currentAnnotationJSON?.data(using: .utf8) else {
            return nil
        }

        do {
            return try decoder.decode(Annotation.self, from: data)
        } catch {
            onError(error)
            return nil
        }
    }
}
