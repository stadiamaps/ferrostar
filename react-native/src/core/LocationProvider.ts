import Geolocation, {
  type GeolocationConfiguration,
  type GeolocationOptions,
} from '@react-native-community/geolocation';
import {
  GeographicCoordinate,
  UserLocation,
  type Heading,
} from '../generated/ferrostar';

export interface LocationProviderInterface {
  lastLocation?: UserLocation;
  lastHeading?: Heading;
  addListener(listener: LocationUpdateListener): void;
  removeListener(listener: LocationUpdateListener): void;
}

export interface LocationUpdateListener {
  onLocationUpdate(location: UserLocation): void;
  onHeadingUpdate(heading: Heading): void;
}

export class LocationProvider implements LocationProviderInterface {
  lastLocation?: UserLocation;
  lastHeading?: Heading;

  private locationUpdateOptions: GeolocationOptions;
  private listeners: Map<LocationUpdateListener, number> = new Map();

  constructor(
    config: GeolocationConfiguration = {
      skipPermissionRequests: false,
      authorizationLevel: 'auto',
      locationProvider: 'auto',
      enableBackgroundLocationUpdates: false,
    },
    options: GeolocationOptions = {
      enableHighAccuracy: true,
      interval: 1000,
      fastestInterval: 0,
    }
  ) {
    this.locationUpdateOptions = options;
    Geolocation.setRNConfiguration(config);
  }

  /**
   * Adds a location update listener.
   *
   * NOTE: This does NOT attempt to check permissions. The caller is responsible for ensuring that
   * permissions are enabled before calling this.
   */
  addListener(listener: LocationUpdateListener): void {
    console.log('LocationProvider', 'Add location listener');
    if (this.listeners.has(listener)) {
      console.log('LocationProvider', 'Already registered; skipping');
      return;
    }

    const watchId = Geolocation.watchPosition(
      (location) => {
        const { coords, timestamp } = location;
        const userLocation = UserLocation.new({
          coordinates: GeographicCoordinate.new({
            lat: coords.latitude,
            lng: coords.longitude,
          }),
          horizontalAccuracy: coords.accuracy,
          // TODO: map these parameters to the correct types not 100% which ones are correct
          courseOverGround: undefined,
          speed:
            coords.speed !== null
              ? {
                  value: coords.speed,
                  accuracy: undefined,
                }
              : undefined,
          timestamp: new Date(timestamp),
        });
        listener.onLocationUpdate(userLocation);
      },
      undefined,
      this.locationUpdateOptions
    );

    this.listeners.set(listener, watchId);
  }

  removeListener(listener: LocationUpdateListener): void {
    console.log('LocationProvider', 'Remove location listener');
    const watchId = this.listeners.get(listener);

    if (watchId === undefined) {
      return;
    }

    Geolocation.clearWatch(watchId);
  }
}

// TODO: Add simulated provider
