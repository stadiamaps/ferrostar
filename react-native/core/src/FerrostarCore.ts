import {
  type GeographicCoordinate,
  type Heading,
  type NavigationSessionLike,
  type Route,
  RouteDeviation,
  UserLocation,
  type Waypoint,
  createNavigationSession,
  NavigationControllerConfig,
  TripState,
  NavState,
  RouteAdapter,
  SpokenInstruction,
} from '@stadiamaps/ferrostar-uniffi-react-native';
import {
  InvalidStatusCodeException,
  NoResponseBodyException,
} from './FerrostarCoreException';
import { getNanoTime, getDistance } from './_utils';
import type { AlternativeRouteProcessor } from './AlternativeRouteProcessor';
import {
  ManualLocationProvider,
  type LocationObserver,
  type LocationProvider,
  type LocationSnapshot,
  type LocationSubscription,
} from './LocationProvider';
import {
  CorrectiveAction,
  type RouteDeviationHandler,
} from './RouteDeviationHandler';
import { type RouteProvider } from './RouteProvider';
import { ManualSpeechEngine, type SpeechEngine } from './SpeechEngine';

/**
 * Represents the complete state of the navigation session.
 */
export class NavigationState {
  static #instance: NavigationState;

  public tripState?: TripState; // We'll keep 'any' here for now to avoid TripState imports until necessary
  public navState?: NavState;
  public routeGeometry: Array<GeographicCoordinate> = [];
  public isCalculatingNewRoute: boolean = false;

  constructor() {}

  /**
   * @deprecated FerrostarCore now owns an instance-scoped NavigationState.
   * This singleton remains only for callers that referenced it directly.
   */
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
export class FerrostarCore implements LocationObserver {
  navigationControllerConfig: NavigationControllerConfig;
  locationProvider: LocationProvider;
  routeProvider: RouteProvider;
  speechEngine: SpeechEngine;

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
  _state: NavigationState = new NavigationState();
  _routeRequestInFlight: boolean = false;
  _lastAutomaticRecalculation?: number;
  _lastRecalculationLocation?: UserLocation;
  _lastLocation?: UserLocation;
  _lastHeading?: Heading;
  _listeners: Map<number, (state: NavigationState) => void> = new Map();
  _isMuted: boolean = false;
  private _nextListenerId: number = 1;
  private _locationSubscription?: LocationSubscription;
  private _locationProviderConnectionId: number = 0;

  constructor(
    navigationControllerConfig: NavigationControllerConfig,
    locationProvider: LocationProvider = new ManualLocationProvider(),
    routeProvider: RouteProvider,
    speechEngine: SpeechEngine = ManualSpeechEngine,
    deviationHandler?: RouteDeviationHandler
  ) {
    this.navigationControllerConfig = navigationControllerConfig;
    this.routeProvider = routeProvider;
    this.locationProvider = locationProvider;
    this.speechEngine = speechEngine;
    this.deviationHandler = deviationHandler;
  }

  async connectLocationProvider(
    locationProvider: LocationProvider
  ): Promise<void> {
    if (
      this.locationProvider === locationProvider &&
      this._locationSubscription
    ) {
      return;
    }

    if (
      this._locationSubscription ||
      this.locationProvider !== locationProvider
    ) {
      await this.disconnectLocationProvider();
    }

    this.locationProvider = locationProvider;
    const connectionId = this._locationProviderConnectionId;
    this.updateLocationSnapshot(locationProvider.getSnapshot?.());

    try {
      const subscription = await locationProvider.subscribe(this);
      if (connectionId !== this._locationProviderConnectionId) {
        await this.unsubscribeLocationSubscription(subscription);
        return;
      }
      this._locationSubscription = subscription;
    } catch (error) {
      this.onLocationError?.(error);
    }
  }

  async disconnectLocationProvider(): Promise<void> {
    this._locationProviderConnectionId += 1;

    const subscription = this._locationSubscription;
    this._locationSubscription = undefined;

    if (subscription) {
      await this.unsubscribeLocationSubscription(subscription);
    }
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

  stopNavigation() {
    const wasNavigating = this._state.isNavigating();

    this._navigationSession = undefined;

    if (wasNavigating) {
      this._state.reset();
      this.notifyStateListeners();
    }

    this._queuedUtteranceIds = [];
    this.speechEngine.stop();
  }

  private speakTTS(spokenInstruction?: SpokenInstruction) {
    if (!spokenInstruction) {
      return;
    }

    if (this._queuedUtteranceIds.includes(spokenInstruction.utteranceId)) {
      return;
    }

    this._queuedUtteranceIds.push(spokenInstruction.utteranceId);
    this.speechEngine.speak(spokenInstruction.text, this._isMuted);
  }

  handleMuted(muted: boolean) {
    this._isMuted = muted;
  }

  private async handleStateUpdate(newState: NavState, location: UserLocation) {
    const tripState = newState.tripState;

    // Send listeners the new state early if we want, or at the end.
    // Android does it via a StateFlow update which is immediate.
    // To match Android exactly, we should update our internal state object FIRST.
    // 1. Guard: Must be navigating
    if (!TripState.Navigating.instanceOf(tripState)) {
      this.notifyStateListeners();
      return;
    }

    const { deviation: routeDeviation, remainingWaypoints } = tripState.inner;

    // 2. Guard: Must be off-route for recalculation logic
    if (!RouteDeviation.OffRoute.instanceOf(routeDeviation)) {
      this.speakTTS(tripState.inner.spokenInstruction);
      this.notifyStateListeners();
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
      this.notifyStateListeners();
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
      this.speakTTS(tripState.inner.spokenInstruction);
      this.notifyStateListeners();
      return;
    }

    // 5. Execute Recalculation
    if (action === CorrectiveAction.GetNewRoutes) {
      this.isCalculatingNewRoute = true;
      this._state.isCalculatingNewRoute = true;
      this.notifyStateListeners();
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
        this._state.isCalculatingNewRoute = false;
        // Final state sync after recalculation attempt
        this.speakTTS(tripState.inner.spokenInstruction);
        this.notifyStateListeners();
      }
    }
  }

  addStateListener(listener: (state: NavigationState) => void): number {
    const id = this._nextListenerId;
    this._nextListenerId += 1;

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
      this.notifyStateListeners();
      return;
    }

    const newState = session.updateUserLocation(location, this._state.navState);

    this._state.set(
      newState,
      this._state.routeGeometry,
      this.isCalculatingNewRoute
    );

    this.handleStateUpdate(newState, location);
  }

  // TODO: remove once we have a way to update the heading
  onHeadingUpdate(heading: Heading): void {
    this._lastHeading = heading;
    this.notifyStateListeners();
  }

  onLocationError(error: unknown): void {
    console.error(`Location provider error: ${error}`);
  }

  // TODO: handle the spoken instructions queue here

  // TODO: foreground service update here

  private updateLocationSnapshot(snapshot?: LocationSnapshot): void {
    if (!snapshot) {
      return;
    }

    if (snapshot.location) {
      this._lastLocation = snapshot.location;
    }
    if (snapshot.heading) {
      this._lastHeading = snapshot.heading;
    }
    this.notifyStateListeners();
  }

  private async unsubscribeLocationSubscription(
    subscription: LocationSubscription
  ): Promise<void> {
    if (typeof subscription === 'function') {
      await subscription();
      return;
    }

    await subscription.unsubscribe();
  }

  private notifyStateListeners(): void {
    this._listeners.forEach((listener) => {
      listener(this._state);
    });
  }
}
