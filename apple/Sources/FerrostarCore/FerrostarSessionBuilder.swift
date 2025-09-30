import FerrostarCoreFFI

public class FerrostarSessionBuilder {
    private var config: SwiftNavigationControllerConfig
    private var caching: NavigationSessionCache?
    private var recorder: NavigationRecorder?
    private var custom: [any NavigationObserver] = []

    /// Determine if there's a valid navigation snapshot.
    /// This is a getter that checks the cache, so it sparingly.
    ///
    /// - Returns: True if there's a fresh snapshot, false otherwise.
    public var canResume: Bool {
        caching?.canResume() ?? false
    }

    /// Create a navigation session builder.
    ///
    /// This class generates navigation sessions inside ``FerrostarCore`` when navigation
    /// starts or resumes.
    ///
    /// - Parameter config: The default Navigation Controller configuration.
    public init(config: SwiftNavigationControllerConfig) {
        self.config = config
    }

    /// Add navigation session caching to the session.
    ///
    /// This can be used to resume navigation from a sessions snapshot.
    ///
    /// - Parameters:
    ///   - config: The caching configuration
    ///   - cache: The platform specific caching. See ``UserDefaultsNavigationCache``
    /// - Returns: The modified session builder.
    public func withCaching(
        config: NavigationCachingConfig,
        cache: any NavigationCache
    ) -> Self {
        caching = NavigationSessionCache(config: config, cache: cache)
        // Load the initial cache, this determines if a fresh-launch of your app can resume.
        _ = caching?.load()
        return self
    }

    /// Add a navigation session recorder to the session.
    ///
    /// The navigation session recorder logs every navigation event for playback
    /// in the Ferrostar web project. This is useful for detailed debugging and
    /// analysis, but should be disabled in production as it's a very expensive system.
    ///
    /// - Parameter recorder: The navigation session recorder.
    /// - Returns: The modified session builder.
    public func withRecorder(_ recorder: NavigationRecorder) -> Self {
        self.recorder = recorder
        return self
    }

    /// Add a Custom navigation observer.
    ///
    /// - Parameter observer: Any ``NavigationObserver``
    /// - Returns: The modified session builder.
    public func withObserver(_ observer: any NavigationObserver) -> Self {
        custom.append(observer)
        return self
    }

    func buildResumedSession() throws(FerrostarCoreError) -> (NavigationSession, Route, NavState) {
        guard let snapshot = caching?.load(), let tripState = snapshot.tripState else {
            // TODO: log is stale.
            throw FerrostarCoreError.noCachedSession
        }

        let route = snapshot.route
        let session = build(for: route)
        let navState = NavState(tripState: tripState, stepAdvanceCondition: config.ffiValue.stepAdvanceCondition)

        return (session, route, navState)
    }

    func build(
        for route: Route,
        with config: NavigationControllerConfig? = nil
    ) -> NavigationSession {
        let observers: [any NavigationObserver] = ([
            caching as NavigationObserver?,
            recorder as NavigationObserver?,
        ] + custom).compactMap { $0 }

        return createNavigationSession(
            route: route,
            config: config ?? self.config.ffiValue,
            observers: observers
        )
    }
}
