import {
  GeographicCoordinate,
  Heading,
  NavigationController,
  NavigationControllerConfig,
  Route,
  RouteDeviation,
  TripState,
  UserLocation,
  Waypoint,
} from '../generated/ferrostar';
import { getNanoTime } from './_utils';
import type { AlternativeRouteProcessor } from './AlternativeRouteProcessor';
import {
  LocationProvider,
  type LocationProviderInterface,
  type LocationUpdateListener,
} from './LocationProvider';
import {
  CorrectiveAction,
  type RouteDeviationHandler,
} from './RouteDeviationHandler';
import type { RouteProviderInterface } from './RouteProvider';
import { RouteProvider } from './RouteProvider';

/**
 * Represents the complete state of the navigation session provided by FerrostarCore-RS
 */
export class NavigationState {
  static #instance: NavigationState;

  public tripState: TripState = TripState.Idle.new();
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
    if (TripState.Navigating.instanceOf(this.tripState.tag)) {
      return true;
    }

    return false;
  }

  set(
    tripState: TripState,
    routeGeometry: Array<GeographicCoordinate>,
    isCalculatingNewRoute: boolean
  ) {
    this.tripState = tripState;
    this.routeGeometry = routeGeometry;
    this.isCalculatingNewRoute = isCalculatingNewRoute;
  }

  reset() {
    this.tripState = TripState.Idle.new();
    this.routeGeometry = [];
    this.isCalculatingNewRoute = false;
  }
}

/**
 * This is the entrypoint for end users of Ferrostar on Android, and is responsible for "driving"
 * the navigation with location updates and other events.
 *
 * The usual flow is for callers to configure an instance of the core reuse the instance for as long
 * as it makes sense (necessarily somewhat app-specific). You can first call [getRoutes] to fetch a
 * list of possible routes asynchronously. After selecting a suitable route (either interactively by
 * the user or programmatically), call [startNavigation] to start a session.
 *
 * NOTE: It is the responsibility of the caller to ensure that the location manager is authorized to
 * access the user's location.
 */
export class FerrostarCore implements LocationUpdateListener {
  navigationControllerConfig: NavigationControllerConfig;
  locationProvider: LocationProviderInterface;
  routeProvider: RouteProviderInterface;

  /**
   * The minimum time to wait before initiating another route recalculation.
   *
   * This matters in the case that a user is off route, the framework calculates a new route, and
   * the user is determined to still be off the new route. This adds a minimum delay (default 5
   * seconds).
   */
  minimumTimeBeforeRecalculaton: number = 5;

  /**
   * Controls what happens when the user deviates from the route.
   *
   * The default behavior (when this property is `null`) is to fetch new routes automatically. These
   * will be passed to the [alternativeRouteProcessor] or, if none is specified, navigation will
   * automatically proceed according to the first route.
   */
  deviationHandler?: RouteDeviationHandler;

  /**
   * Handles alternative routes as they are loaded.
   *
   * The default behavior (when this property is `null`) is to automatically reroute the user when
   * an alternative route arrives due to the user being off course. In all other cases, no action
   * will be taken unless an [AlternativeRouteProcessor] is provided.
   */
  alternativeRouteProcessor?: AlternativeRouteProcessor;

  // Maintains a set of utterance IDs which been seen previously.
  // This helps us maintian the guarantee that the observer won't see the same one twice.
  _queuedUtteranceIds: Array<string> = [];

  isCalculatingNewRoute: boolean = false;

  _navigationController?: NavigationController;
  _state: NavigationState = NavigationState.instance();
  _routeRequestInFlight: boolean = false;
  _lastAutomaticRecalculation?: number;
  _lastLocation?: UserLocation;
  _listeners: Map<number, (state: NavigationState) => void> = new Map();

  constructor(
    valhallaEndpointURL: string,
    profile: string,
    navigationControllerConfig: NavigationControllerConfig,
    options: Record<string, any> = {},
    locationProvider: LocationProviderInterface = new LocationProvider(),
    routeProvider: RouteProviderInterface = new RouteProvider(
      valhallaEndpointURL,
      profile,
      options
    )
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

      return await this.routeProvider.getRoute(initialLocation, waypoints);
    } catch (e) {
      console.log(`Failed to get routes: ${e}`);
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
   * off course, use [replaceRoute] instead! Otherwise, you will miss out on updates as the old view
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
    const controller = new NavigationController(
      route,
      this.navigationControllerConfig
    );

    const firstRouteLocation = route.geometry[0];
    if (firstRouteLocation === undefined) {
      return;
    }

    const startingLocation =
      this.locationProvider.lastLocation ??
      UserLocation.new({
        coordinates: firstRouteLocation,
        horizontalAccuracy: 0.0,
        courseOverGround: undefined,
        timestamp: new Date(),
        speed: undefined,
      });

    const initialTripState = controller.getInitialState(startingLocation);

    this._navigationController = controller;
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

    const controller = new NavigationController(
      route,
      this.navigationControllerConfig
    );

    const firstRouteLocation = route.geometry[0];
    if (firstRouteLocation === undefined) {
      return;
    }

    const startingLocation =
      this.locationProvider.lastLocation ??
      UserLocation.new({
        coordinates: firstRouteLocation,
        horizontalAccuracy: 0.0,
        courseOverGround: undefined,
        timestamp: new Date(),
        speed: undefined,
      });

    this._navigationController = controller;
    const newState = controller.getInitialState(startingLocation);
    this._state.set(newState, route.geometry, false);

    this.handleStateUpdate(newState, startingLocation);
  }

  advanceToNextStep() {
    const controller = this._navigationController;
    const location = this._lastLocation;

    if (controller === undefined || location === undefined) {
      return;
    }

    const newState = controller.advanceToNextStep(this._state.tripState);
    this._state.set(
      newState,
      this._state.routeGeometry,
      this.isCalculatingNewRoute
    );

    this.handleStateUpdate(newState, location);
  }

  stopNavigation(stopLocationUpdates: boolean = true) {
    if (stopLocationUpdates) {
      this.locationProvider.removeListener(this);
    }
    this._navigationController?.uniffiDestroy();
    this._navigationController = undefined;
    this._state.reset();
    // TODO: handle state change event here
    // Send listeners the new state
    this._listeners.forEach((listener) => {
      listener(this._state);
    });

    this._queuedUtteranceIds = [];
    // TODO: add TTS observer to clear queued utterances
  }

  private async handleStateUpdate(newState: TripState, location: UserLocation) {
    // If we're not navigating, we don't care about state changes.
    if (!TripState.Navigating.instanceOf(newState)) {
      return;
    }

    // If we're not recalculating a new route, we don't care about state changes.
    if (RouteDeviation.OffRoute.instanceOf(newState.inner.deviation)) {
      // Check that the last automatic recalculation wasn't too recent.
      // We have to do some weird thing here with hrTime since JavaScript doesn't have a nice nanoseoncds method.
      const isGreaterThanMinimumTime = this._lastAutomaticRecalculation
        ? getNanoTime() - this._lastAutomaticRecalculation >
          this.minimumTimeBeforeRecalculaton
        : true;

      if (this._routeRequestInFlight || !isGreaterThanMinimumTime) {
        return;
      }

      const action =
        this.deviationHandler?.correctiveActionForDeviation(
          this,
          newState.inner.deviation.inner.deviationFromRouteLine,
          newState.inner.remainingWaypoints
        ) ?? CorrectiveAction.GetNewRoutes;

      switch (action) {
        case CorrectiveAction.DoNothing:
          break;
        case CorrectiveAction.GetNewRoutes:
          this.isCalculatingNewRoute = true;
          try {
            const routes = await this.getRoutes(
              location,
              newState.inner.remainingWaypoints
            );
            const config = this.navigationControllerConfig;
            const processor = this.alternativeRouteProcessor;
            const state = this._state;
            // Make sure we are still navigating and the new route is still relevant.
            if (
              TripState.Navigating.instanceOf(state.tripState) &&
              RouteDeviation.OffRoute.instanceOf(
                state.tripState.inner.deviation
              )
            ) {
              if (processor !== undefined) {
                processor.loadedAlternativeRoutes(this, routes);
              } else if (routes.length > 0) {
                // Default behavior when there is no user-defined behavior:
                // accept the first route, as this is what most users want when they go off route.
                const firstRoute = routes[0];
                // Stupid TS can't figure out that firstRoute is not undefined here.
                if (firstRoute === undefined) {
                  throw new Error('No route found');
                }

                this.replaceRoute(firstRoute, config);
              }
            }
          } catch (e) {
            console.log(`Failed to recalculate route: ${e}`);
          } finally {
            this._lastAutomaticRecalculation = getNanoTime();
            this.isCalculatingNewRoute = false;
          }
          break;
      }
    }

    // Send listeners the new state
    this._listeners.forEach((listener) => {
      listener(this._state);
    });
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
    const controller = this._navigationController;

    if (controller === undefined) {
      return;
    }

    const newState = controller.updateUserLocation(
      location,
      this._state.tripState
    );

    this.handleStateUpdate(newState, location);

    this._state.set(
      newState,
      this._state.routeGeometry,
      this.isCalculatingNewRoute
    );
  }

  // TODO: remove once we have a way to update the heading
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onHeadingUpdate(heading: Heading): void {
    // TODO: heading update
  }

  // TODO: handle the spoken instructions queue here

  // TODO: foreground service update here
}
