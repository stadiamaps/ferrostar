import FerrostarCoreFFI

public class FerrostarSessionBuilder {
    private var config: SwiftNavigationControllerConfig
    private var caching: NavigationSessionCache?
    private var recorder: NavigationRecorder?
    private var custom: [any NavigationObserver] = []

    public var canResume: Bool {
        caching?.canResume() ?? false
    }

    public init(config: SwiftNavigationControllerConfig) {
        self.config = config
    }

    public func withCaching(
        config: NavigationCachingConfig,
        cache: any NavigationCache
    ) -> Self {
        caching = NavigationSessionCache(config: config, cache: cache)
        // Load the initial cache, this determines if a fresh-launch of your app can resume.
        _ = caching?.load()
        return self
    }

    public func withRecording(_ recorder: NavigationRecorder) -> Self {
        self.recorder = recorder
        return self
    }

    public func withCustom(_ observer: any NavigationObserver) -> Self {
        custom.append(observer)
        return self
    }

    func createResume() throws(FerrostarCoreError) -> (NavigationSession, Route, NavState) {
        guard let record = caching?.load(), let tripState = record.tripState else {
            // TODO: log is stale.
            throw FerrostarCoreError.notPossible("No cached navigation session to resume from.")
        }

        let route = record.route
        let session = create(for: route)
        let navState = NavState(tripState: tripState, stepAdvanceCondition: config.ffiValue.stepAdvanceCondition)

        return (session, route, navState)
    }

    func create(
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
