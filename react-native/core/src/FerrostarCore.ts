import {
  type GeographicCoordinate,
  type Heading,
  type NavigationObserver,
  type NavigationSessionLike,
  type Route,
  RouteDeviation,
  UserLocation,
  type Waypoint,
  createNavigationSession,
  NavigationControllerConfig,
  NavigationController,
  TripState,
  NavState,
  RouteAdapter,
} from '@stadiamaps/ferrostar-uniffi-react-native';
import {
  InvalidStatusCodeException,
  NoResponseBodyException,
} from './FerrostarCoreException';
import { getNanoTime, ab2json, getDistance } from './_utils';
import type { AlternativeRouteProcessor } from './AlternativeRouteProcessor';
import {
  ManualLocationProvider,
  type LocationProviderInterface,
  type LocationUpdateListener,
} from './LocationProvider';
import {
  CorrectiveAction,
  type RouteDeviationHandler,
} from './RouteDeviationHandler';
import { type RouteProvider } from './RouteProvider';

/**
 * Represents the complete state of the navigation session.
 */
export class NavigationState {
  static #instance: NavigationState;

  public tripState?: TripState; // We'll keep 'any' here for now to avoid TripState imports until necessary
  public navState?: NavState;
  public routeGeometry: Array<GeographicCoordinate> = [];
  public isCalculatingNewRoute: boolean = false;

  private constructor() {}

  static instance(): NavigationState {
    if (this.#instance) {
      return this.#instance;
    }
    this.#instance = new NavigationState();
    return this.#instance;
  }

  isNavigating(): boolean {
    if (this.tripState && this.tripState.tag === 'Navigating') {
      return true;
    }

    return false;
  }

  set(
    navState: NavState,
    routeGeometry: Array<GeographicCoordinate>,
    isCalculatingNewRoute: boolean
  ) {
    this.navState = navState;
    this.tripState = navState.tripState;
    this.routeGeometry = routeGeometry;
    this.isCalculatingNewRoute = isCalculatingNewRoute;
  }

  reset() {
    this.navState = undefined;
    this.tripState = undefined;
    this.routeGeometry = [];
    this.isCalculatingNewRoute = false;
  }
}

/**
 * This is the entrypoint for end users of Ferrostar on React Native, and is responsible for "driving"
 * the navigation with location updates and other events.
 *
 * The usual flow is for callers to configure an instance of the core reuse the instance for as long
 * as it makes sense (necessarily somewhat app-specific). You can first call {@link getRoutes} to fetch a
 * list of possible routes asynchronously. After selecting a suitable route (either interactively by
 * the user or programmatically), call {@link startNavigation} to start a session.
 *
 * NOTE: It is the responsibility of the caller to ensure that the location manager is authorized to
 * access the user's location.
 */
export class FerrostarCore implements LocationUpdateListener {
  navigationControllerConfig: NavigationControllerConfig;
  locationProvider: LocationProviderInterface;
  routeProvider: RouteProvider;

  /**
   * The minimum time to wait before initiating another route recalculation.
   *
   * This matters in the case that a user is off route, the framework calculates a new route, and
   * the user is determined to still be off the new route. This adds a minimum delay (default 5
   * seconds).
   */
  minimumTimeBeforeRecalculation: number = 5;

  /**
   * Controls what happens when the user deviates from the route.
   *
   * The default behavior (when this property is `null`) is to fetch new routes automatically. These
   * will be passed to the {@link AlternativeRouteProcessor} or, if none is specified, navigation will
   * automatically proceed according to the first route.
   */
  deviationHandler?: RouteDeviationHandler;

  /**
   * Handles alternative routes as they are loaded.
   *
   * The default behavior (when this property is `null`) is to automatically reroute the user when
   * an alternative route arrives due to the user being off course. In all other cases, no action
   * will be taken unless an {@link AlternativeRouteProcessor} is provided.
   */
  alternativeRouteProcessor?: AlternativeRouteProcessor;

  // Maintains a set of utterance IDs which been seen previously.
  // This helps us maintain the guarantee that the observer won't see the same one twice.
  _queuedUtteranceIds: Array<string> = [];

  isCalculatingNewRoute: boolean = false;

  _navigationSession?: NavigationSessionLike;
  _state: NavigationState = NavigationState.instance();
  _routeRequestInFlight: boolean = false;
  _lastAutomaticRecalculation?: number;
  _lastRecalculationLocation?: UserLocation;
  _lastLocation?: UserLocation;
  _listeners: Map<number, (state: NavigationState) => void> = new Map();

  constructor(
    navigationControllerConfig: NavigationControllerConfig,
    locationProvider: LocationProviderInterface = new ManualLocationProvider(),
    routeProvider: RouteProvider
  ) {
    this.navigationControllerConfig = navigationControllerConfig;
    this.routeProvider = routeProvider;
    this.locationProvider = locationProvider;
  }

  async getRoutes(
    initialLocation: UserLocation,
    waypoints: Array<Waypoint>
  ): Promise<Array<Route>> {
    try {
      this._routeRequestInFlight = true;

      if (this.routeProvider.kind === 'custom') {
        return await this.routeProvider.getRoutes(initialLocation, waypoints);
      }

      if (this.routeProvider.kind === 'adapter') {
        const adapter = RouteAdapter.fromWellKnownRouteProvider(
          this.routeProvider.provider
        );
        const request = adapter.generateRequest(initialLocation, waypoints);

        if (request.tag !== 'HttpPost') {
          throw new Error(`Unsupported route request type: ${request.tag}`);
        }

        const fetchHeaders: Record<string, string> = {};
        if (request.inner.headers) {
          request.inner.headers.forEach((value, key) => {
            fetchHeaders[key] = value;
          });
        }

        const response = await fetch(request.inner.url, {
          method: 'POST',
          headers: fetchHeaders,
          body: new Uint8Array(request.inner.body),
        });

        if (!response.ok) {
          throw new InvalidStatusCodeException(response.status);
        }

        const arrayBuffer = await response.arrayBuffer();
        if (!arrayBuffer || arrayBuffer.byteLength === 0) {
          throw new NoResponseBodyException();
        }
        return adapter.parseResponse(arrayBuffer);
      }

      throw new Error('Unknown route provider kind');
    } catch (e: unknown) {
      if (e instanceof InvalidStatusCodeException) {
        console.warn(`Failed to get routes: Status ${e.message}`);
      } else {
        console.error(`Failed to get routes: ${e}`);
      }
      return [];
    } finally {
      this._routeRequestInFlight = false;
    }
  }

  /**
   * Starts a navigation session with the given parameters (erasing any previous state).
   *
   * Once you have a location fix and a desired route, invoke this method. It will automatically
   * subscribe to location provider updates. Returns a view model which is tied to the navigation
   * session. You can observe this in either your own or one of the provided navigation compose
   * views.
   *
   * WARNING: If you want to reuse the existing view model, ex: when getting a new route after going
   * off course, use {@link replaceRoute} instead! Otherwise, you will miss out on updates as the old view
   * model is "orphaned"!
   *
   * @param route the route to navigate.
   * @param config change the configuration in the core before staring navigation. This was
   *   originally provided on init, but you can set a new value for future sessions.
   * @throws UserLocationUnknown if the location provider has no last known location.
   */
  startNavigation(route: Route, config?: NavigationControllerConfig) {
    this.stopNavigation();

    this.navigationControllerConfig = config ?? this.navigationControllerConfig;
    const session = createNavigationSession(
      route,
      config ?? this.navigationControllerConfig,
      []
    );

    const startingLocation =
      this._lastLocation ??
      UserLocation.new({
        coordinates: { lat: 0, lng: 0 },
        horizontalAccuracy: 0,
        courseOverGround: undefined,
        timestamp: new Date(),
        speed: undefined,
      });

    const initialTripState = session.getInitialState(startingLocation);

    this._navigationSession = session;
    this._state.set(initialTripState, route.geometry, false);

    this.handleStateUpdate(initialTripState, startingLocation);

    // Add location provider listener here
    this.locationProvider.addListener(this);
  }

  /**
   * Replace the currently running route with a new one.
   *
   * This allows you to reuse the existing view model. Do not call this method unless you are
   * already navigating.
   *
   * @param route the route to navigate.
   * @param config change the configuration in the core before staring navigation. This was
   *   originally provided on init, but you can set a new value for future sessions.
   */
  replaceRoute(route: Route, config?: NavigationControllerConfig) {
    this.navigationControllerConfig = config ?? this.navigationControllerConfig;

    const session = createNavigationSession(
      route,
      config ?? this.navigationControllerConfig,
      []
    );

    const startingLocation =
      this._lastLocation ??
      UserLocation.new({
        coordinates: { lat: 0, lng: 0 },
        horizontalAccuracy: 0,
        courseOverGround: undefined,
        timestamp: new Date(),
        speed: undefined,
      });

    this._navigationSession = session;
    const newState = session.getInitialState(startingLocation);
    this._state.set(newState, route.geometry, false);

    this.handleStateUpdate(newState, startingLocation);
  }

  advanceToNextStep() {
    const session = this._navigationSession;
    const location = this._lastLocation;

    if (
      session === undefined ||
      location === undefined ||
      this._state.navState === undefined
    ) {
      return;
    }

    const newState = session.advanceToNextStep(this._state.navState);
    this._state.set(
      newState,
      this._state.routeGeometry,
      this.isCalculatingNewRoute
    );

    this.handleStateUpdate(newState, location);
  }

  stopNavigation(stopLocationUpdates: boolean = true) {
    if (!this._state.isNavigating()) {
      return;
    }

    this._navigationSession = undefined;
    this._state.reset();
    // TODO: handle state change event here
    // Send listeners the new state
    this._listeners.forEach((listener) => {
      listener(this._state);
    });

    this._queuedUtteranceIds = [];
    // TODO: add TTS observer to clear queued utterances
  }

  private async handleStateUpdate(newState: NavState, location: UserLocation) {
    const tripState = newState.tripState;

    // Send listeners the new state early if we want, or at the end.
    // Android does it via a StateFlow update which is immediate.
    // To match Android exactly, we should update our internal state object FIRST.
    // 1. Guard: Must be navigating
    if (!TripState.Navigating.instanceOf(tripState)) {
      this._listeners.forEach((listener) => listener(this._state));
      return;
    }

    const { deviation: routeDeviation, remainingWaypoints } = tripState.inner;

    // 2. Guard: Must be off-route for recalculation logic
    if (!RouteDeviation.OffRoute.instanceOf(routeDeviation)) {
      this._listeners.forEach((listener) => listener(this._state));
      return;
    }

    // 3. Guard: Check throttles and flight status
    const now = getNanoTime();
    const hasWaited = this._lastAutomaticRecalculation
      ? now - this._lastAutomaticRecalculation >
        this.minimumTimeBeforeRecalculation * 1000000000
      : true;

    const hasMovedSignificantly = this._lastRecalculationLocation
      ? getDistance(
          location.coordinates,
          this._lastRecalculationLocation.coordinates
        ) > 50.0
      : true;

    if (this._routeRequestInFlight || !hasWaited || !hasMovedSignificantly) {
      this._listeners.forEach((listener) => listener(this._state));
      return;
    }

    // 4. Determine corrective action
    const action =
      this.deviationHandler?.correctiveActionForDeviation(
        this,
        routeDeviation.inner.deviationFromRouteLine,
        remainingWaypoints
      ) ?? CorrectiveAction.GetNewRoutes;

    if (action === CorrectiveAction.DoNothing) {
      this._listeners.forEach((listener) => listener(this._state));
      return;
    }

    // 5. Execute Recalculation
    if (action === CorrectiveAction.GetNewRoutes) {
      this.isCalculatingNewRoute = true;
      this._lastRecalculationLocation = location;
      try {
        const routes = await this.getRoutes(location, remainingWaypoints);
        const config = this.navigationControllerConfig;
        const processor = this.alternativeRouteProcessor;

        // Verify we are still in a state that needs this new route
        if (
          TripState.Navigating.instanceOf(this._state.tripState) &&
          RouteDeviation.OffRoute.instanceOf(
            this._state.tripState.inner.deviation
          )
        ) {
          if (processor !== undefined) {
            processor.loadedAlternativeRoutes(this, routes);
          } else if (routes.length > 0) {
            const firstRoute = routes[0];
            if (firstRoute) {
              this.replaceRoute(firstRoute, config);
            }
          }
        }
      } catch (e) {
        console.log(`Failed to recalculate route: ${e}`);
      } finally {
        this._lastAutomaticRecalculation = getNanoTime();
        this.isCalculatingNewRoute = false;
        // Final state sync after recalculation attempt
        this._listeners.forEach((listener) => listener(this._state));
      }
    }
  }

  addStateListener(listener: (state: NavigationState) => void): number {
    // Create id for listener
    const id = this._listeners.size + 1;

    this._listeners.set(id, listener);

    return id;
  }

  removeStateListener(id: number): void {
    this._listeners.delete(id);
  }

  onLocationUpdate(location: UserLocation): void {
    this._lastLocation = location;
    const session = this._navigationSession;

    if (session === undefined || this._state.navState === undefined) {
      return;
    }

    const newState = session.updateUserLocation(location, this._state.navState);

    this.handleStateUpdate(newState, location);

    this._state.set(
      newState,
      this._state.routeGeometry,
      this.isCalculatingNewRoute
    );
  }

  // TODO: remove once we have a way to update the heading
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onHeadingUpdate(_heading: Heading): void {
    // TODO: heading update
  }

  // TODO: handle the spoken instructions queue here

  // TODO: foreground service update here
}
