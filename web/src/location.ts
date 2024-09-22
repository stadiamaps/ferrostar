import { advanceLocationSimulation, locationSimulationFromRoute } from "@stadiamaps/ferrostar";

export function ferrostarUserLocation(position: GeolocationPosition): object {
  let speed = null;
  if (position.coords.speed) {
    speed = {
      value: position.coords.speed
    }
  }

  return {
    coordinates: { lat: position.coords.latitude, lng: position.coords.longitude },
    horizontalAccuracy: position.coords.accuracy,
    courseOverGround: {
      degrees: Math.floor(position.coords.heading || 0),
    },
    timestamp: position.timestamp,
    speed: speed,
  };
}

export class SimulatedLocationProvider {
  private simulationState = null;
  private isRunning = false;

  lastLocation = null;
  lastHeading = null;
  warpFactor = 1;

  updateCallback: () => void = () => {};

  setSimulatedRoute(route: any) {
    this.simulationState = locationSimulationFromRoute(route, 10.0);
    this.start();
  }

  async start() {
    if (this.isRunning) { return; }
    this.isRunning = true;

    while (this.simulationState !== null) {
      await new Promise((resolve) => setTimeout(resolve, (1 / this.warpFactor) * 1000));
      const initialState = this.simulationState;
      const updatedState = advanceLocationSimulation(initialState);

      if (initialState === updatedState) {
        return;
      }

      this.simulationState = updatedState;
      this.lastLocation = updatedState.current_location;

      // Since Lit cannot detect changes inside objects, here we use a callback to trigger a re-render
      // This is a minimal approach if we don't want to use a state management library like MobX, but might not be the ideal solution
      if (this.updateCallback) {
        this.updateCallback();
      }
    }

    this.isRunning = false;
  }

  stop() {
    this.simulationState = null;
    // Note: this.isRunning is intentionally not set here.
    // It will be set naturally as the start function eventually exits.
  }
}

export class BrowserLocationProvider {
  private geolocationWatchId: number | null = null;
  lastLocation: any = null;
  lastHeading = null;

  updateCallback: () => void = () => {};

  /**
   * Starts location updates in the background.
   *
   * Whenever the user's location is updated,
   * the `lastLocation` property will reflect the result
   * Additionally, the `updateCallback` will be invoked,
   * which provides a way for a single subscriber to get updates.
   */
  start() {
    if (navigator.geolocation && !this.geolocationWatchId) {
      const options = {
        enableHighAccuracy: true,
      };

      this.geolocationWatchId = navigator.geolocation.watchPosition((position: GeolocationPosition) => {
          this.lastLocation = ferrostarUserLocation(position);
          if (this.updateCallback) {
            this.updateCallback();
          }
        },
        // TODO: Better alert mechanism
        (error) => {
          this.geolocationWatchId = null;
          alert(error.message);
        },
        options
      );
    }
  }

  /**
   * Gets the current location of the user asynchronously.
   * If a cached location is available from within the last 30 seconds,
   * it will be returned instantly.
   * If no cached location is available,
   * the Geolocation API will be used to fetch one.
   */
  getCurrentLocation(): Promise<object> {
    if (!navigator.geolocation) {
      return new Promise<object>((_, reject) => {
        reject("This navigator does not support geolocation.")
      });
    }

    const thirtySecondsAgo = new Date().getTime() - 30_000;
    if (this.lastLocation && this.lastLocation.timestamp > thirtySecondsAgo) {
      console.log("Using cached location");
      return this.lastLocation;
    } else {
      return new Promise<object>((resolve, reject) => {
        console.log("Waiting for location.");
        navigator.geolocation.getCurrentPosition((position: GeolocationPosition) => {
          console.log("Received location.");
          const userLocation = ferrostarUserLocation(position);
          this.lastLocation = userLocation;
          resolve(userLocation);
        }, reject);
      });
    }
  }

  /**
   * Stops location updates.
   */
  stop() {
    this.lastLocation = null;
    if (navigator.geolocation && this.geolocationWatchId) {
      navigator.geolocation.clearWatch(this.geolocationWatchId);
      this.geolocationWatchId = null;
    }
  }
}
