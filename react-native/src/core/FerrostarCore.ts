import {
  GeographicCoordinate,
  NavigationControllerConfig,
  Route,
  TripState,
  UserLocation,
  Waypoint,
} from '../generated/ferrostar';
import type { RouteProviderInterface } from './RouteProvider';
import { RouteProvider } from './RouteProvider';

/**
 * Represents the complete state of the navigation session provided bt FerrostarCore-RS
 */
export class NavigationState {
  static #instance: NavigationState;

  public static tripState: TripState = TripState.Idle.new();
  public static routeGeometry: Array<GeographicCoordinate> = [];
  public static isCalculatingNewRoute: boolean = false;

  private constructor() {}

  static instance(): NavigationState {
    if (this.#instance) {
      return this.#instance;
    }
    this.#instance = new NavigationState();
    return this.#instance;
  }

  static isNavigating(): boolean {
    if (TripState.Navigating.instanceOf(this.tripState.tag)) {
      return true;
    }

    return false;
  }

  static set(
    tripState: TripState,
    routeGeometry: Array<GeographicCoordinate>,
    isCalculatingNewRoute: boolean
  ) {
    this.tripState = tripState;
    this.routeGeometry = routeGeometry;
    this.isCalculatingNewRoute = isCalculatingNewRoute;
  }
}

export class FerrostarCore {
  navigationControllerConfig: NavigationControllerConfig;
  routeProvider: RouteProviderInterface;

  _routeRequestInFlight: boolean = false;

  constructor(
    valhallaEndpointURL: string,
    profile: string,
    navigationControllerConfig: NavigationControllerConfig,
    options: Record<string, any> = {},
    routeProvider: RouteProviderInterface = new RouteProvider(
      valhallaEndpointURL,
      profile,
      options
    )
  ) {
    this.navigationControllerConfig = navigationControllerConfig;
    this.routeProvider = routeProvider;
  }

  async getRoutes(
    initialLocation: UserLocation,
    waypoints: Array<Waypoint>
  ): Promise<Array<Route>> {
    try {
      this._routeRequestInFlight = true;

      return await this.routeProvider.getRoute(initialLocation, waypoints);
    } finally {
      this._routeRequestInFlight = false;
    }
  }
}
