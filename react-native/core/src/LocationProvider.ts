import {
  UserLocation,
  type Heading,
  type Route,
  type LocationSimulationState,
  LocationBias,
  advanceLocationSimulation,
  locationSimulationFromRoute,
} from '@stadiamaps/ferrostar-uniffi-react-native';

export type MaybePromise<T> = T | Promise<T>;

export type LocationSnapshot = {
  location?: UserLocation;
  heading?: Heading;
};

export interface LocationObserver {
  onLocationUpdate(location: UserLocation): void;
  onHeadingUpdate?(heading: Heading): void;
  onLocationError?(error: unknown): void;
}

export type LocationSubscription =
  | { unsubscribe(): MaybePromise<void> }
  | (() => MaybePromise<void>);

export interface LocationProvider {
  getSnapshot?(): LocationSnapshot | undefined;
  subscribe(observer: LocationObserver): MaybePromise<LocationSubscription>;
}

export class ManualLocationProvider implements LocationProvider {
  private lastLocation?: UserLocation;
  private lastHeading?: Heading;

  private observers: Set<LocationObserver> = new Set();

  constructor() {}

  subscribe(observer: LocationObserver): LocationSubscription {
    this.observers.add(observer);

    return () => {
      this.observers.delete(observer);
    };
  }

  getSnapshot(): LocationSnapshot {
    return {
      location: this.lastLocation,
      heading: this.lastHeading,
    };
  }

  updateLocation(location: UserLocation): void {
    this.lastLocation = location;
    this.observers.forEach((observer) => {
      observer.onLocationUpdate(location);
    });
  }

  updateHeading(heading: Heading): void {
    this.lastHeading = heading;
    this.observers.forEach((observer) => {
      observer.onHeadingUpdate?.(heading);
    });
  }
}

/**
 * A location provider that simulates progress along a route.
 *
 * This is useful for testing and demonstrations without having to physically move the device.
 */
export class SimulatedLocationProvider implements LocationProvider {
  private lastLocation?: UserLocation;
  private lastHeading?: Heading;
  private _warpFactor: number = 1;

  private observers: Set<LocationObserver> = new Set();
  private simulationState?: LocationSimulationState;
  private intervalId?: ReturnType<typeof setInterval>;
  private isPendingCompletion: boolean = false;

  constructor(initialLocation?: UserLocation) {
    this.lastLocation = initialLocation;
    if (initialLocation?.courseOverGround) {
      this.lastHeading = {
        trueHeading: initialLocation.courseOverGround.degrees,
        accuracy: initialLocation.courseOverGround.accuracy ?? 0,
        timestamp: initialLocation.timestamp,
      };
    }
  }

  /**
   * The factor by which the simulation speed is multiplied.
   *
   * A warp factor of 2 will simulate movement at twice the normal speed.
   */
  get warpFactor(): number {
    return this._warpFactor;
  }

  set warpFactor(value: number) {
    this._warpFactor = value;
    if (this.intervalId) {
      this.stop();
      this.start();
    }
  }

  subscribe(observer: LocationObserver): LocationSubscription {
    this.observers.add(observer);

    return () => {
      this.observers.delete(observer);
    };
  }

  getSnapshot(): LocationSnapshot {
    return {
      location: this.lastLocation,
      heading: this.lastHeading,
    };
  }

  /**
   * Sets the route to simulate.
   *
   * @param route The route to simulate progress along.
   * @param resampleDistance The maximum distance (in meters) between simulated points.
   * @param bias The location bias to apply to the simulated locations.
   */
  setRoute(
    route: Route,
    resampleDistance: number = 10,
    bias: LocationBias = new LocationBias.None()
  ): void {
    this.stop();
    this.simulationState = locationSimulationFromRoute(
      route,
      resampleDistance,
      bias
    );
    this.isPendingCompletion = false;
    this.updateFromState(this.simulationState);
    this.start();
  }

  /**
   * Starts the simulation.
   */
  start(): void {
    if (this.intervalId || !this.simulationState) {
      return;
    }

    this.intervalId = setInterval(() => {
      if (!this.simulationState) {
        this.stop();
        return;
      }

      const nextState = advanceLocationSimulation(this.simulationState);

      // Check if we've reached the end of the route.
      // The Rust core returns the same state when the simulation is finished.
      if (
        nextState.remainingLocations.length ===
        this.simulationState.remainingLocations.length
      ) {
        if (this.isPendingCompletion) {
          this.stop();
          return;
        } else {
          this.isPendingCompletion = true;
        }
      }

      this.simulationState = nextState;
      this.updateFromState(this.simulationState);
    }, 1000 / this._warpFactor);
  }

  /**
   * Stops the simulation.
   */
  stop(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = undefined;
    }
  }

  updateLocation(location: UserLocation): void {
    if (this.intervalId) {
      // Ignore manual updates while simulating
      return;
    }
    this.lastLocation = location;
    this.observers.forEach((observer) => {
      observer.onLocationUpdate(location);
    });
  }

  updateHeading(heading: Heading): void {
    if (this.intervalId) {
      // Ignore manual updates while simulating
      return;
    }
    this.lastHeading = heading;
    this.observers.forEach((observer) => {
      observer.onHeadingUpdate?.(heading);
    });
  }

  private updateFromState(state: LocationSimulationState): void {
    const location = state.currentLocation;
    this.lastLocation = location;

    if (location.courseOverGround) {
      this.lastHeading = {
        trueHeading: location.courseOverGround.degrees,
        accuracy: location.courseOverGround.accuracy ?? 0,
        timestamp: location.timestamp,
      };
    }

    this.observers.forEach((observer) => {
      observer.onLocationUpdate(location);
      if (this.lastHeading) {
        observer.onHeadingUpdate?.(this.lastHeading);
      }
    });
  }
}
