import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@stadiamaps/ferrostar-uniffi-react-native', () => ({
  createNavigationSession: vi.fn(),
  NavigationControllerConfig: class NavigationControllerConfig {},
  NavigationController: class NavigationController {},
  LocationBias: {
    None: class None {},
  },
  RouteAdapter: {
    fromWellKnownRouteProvider: vi.fn(),
  },
  RouteDeviation: {
    OffRoute: {
      instanceOf: (value: { tag?: string } | undefined) =>
        value?.tag === 'OffRoute',
    },
  },
  TripState: {
    Navigating: {
      instanceOf: (value: { tag?: string } | undefined) =>
        value?.tag === 'Navigating',
    },
  },
  UserLocation: {
    new: (location: unknown) => location,
  },
  advanceLocationSimulation: vi.fn(),
  locationSimulationFromRoute: vi.fn(),
}));

import { createNavigationSession } from '@stadiamaps/ferrostar-uniffi-react-native';
import { FerrostarCore } from '../FerrostarCore';
import {
  ManualLocationProvider,
  SimulatedLocationProvider,
  type LocationObserver,
  type LocationProvider,
  type LocationSnapshot,
  type LocationSubscription,
} from '../LocationProvider';
import type { RouteProvider } from '../RouteProvider';

class FakeLocationProvider implements LocationProvider {
  snapshot?: LocationSnapshot;
  observers = new Set<LocationObserver>();
  unsubscribeCalls = 0;

  subscribe(observer: LocationObserver): LocationSubscription {
    this.observers.add(observer);

    return () => {
      this.unsubscribeCalls += 1;
      this.observers.delete(observer);
    };
  }

  getSnapshot(): LocationSnapshot | undefined {
    return this.snapshot;
  }

  emitLocation(location: any): void {
    this.snapshot = {
      ...this.snapshot,
      location,
    };
    this.observers.forEach((observer) => observer.onLocationUpdate(location));
  }
}

class AsyncFakeLocationProvider extends FakeLocationProvider {
  resolveSubscribe?: (subscription: LocationSubscription) => void;

  subscribe(observer: LocationObserver): Promise<LocationSubscription> {
    this.observers.add(observer);

    return new Promise((resolve) => {
      this.resolveSubscribe = (subscription) => resolve(subscription);
    });
  }
}

const createRouteProvider = (): RouteProvider => ({
  kind: 'custom',
  getRoutes: vi.fn(),
});

const createRoute = () =>
  ({
    geometry: [{ lat: 1, lng: 2 }],
  }) as any;

const createLocation = (id = 'location') =>
  ({
    id,
    coordinates: { lat: 1, lng: 2 },
    horizontalAccuracy: 5,
    timestamp: new Date(0),
  }) as any;

const createNavState = (id: string) =>
  ({
    id,
    tripState: {
      tag: 'Navigating',
      inner: {
        deviation: { tag: 'OnRoute' },
        remainingWaypoints: [],
      },
    },
  }) as any;

const mockSession = (initialState = createNavState('initial')) => {
  const session = {
    getInitialState: vi.fn(() => initialState),
    updateUserLocation: vi.fn(() => createNavState('updated')),
    advanceToNextStep: vi.fn(),
  };

  vi.mocked(createNavigationSession).mockReturnValue(session as any);
  return session;
};

describe('LocationProvider implementations', () => {
  it('ManualLocationProvider subscribe cleanup removes observer', () => {
    const provider = new ManualLocationProvider();
    const observer = {
      onLocationUpdate: vi.fn(),
    };
    const subscription = provider.subscribe(observer);

    provider.updateLocation(createLocation('first'));
    expect(observer.onLocationUpdate).toHaveBeenCalledTimes(1);

    if (typeof subscription === 'function') {
      subscription();
    }

    provider.updateLocation(createLocation('second'));
    expect(observer.onLocationUpdate).toHaveBeenCalledTimes(1);
  });

  it('SimulatedLocationProvider subscribe cleanup removes observer', () => {
    const provider = new SimulatedLocationProvider();
    const observer = {
      onLocationUpdate: vi.fn(),
    };
    const subscription = provider.subscribe(observer);

    provider.updateLocation(createLocation('first'));
    expect(observer.onLocationUpdate).toHaveBeenCalledTimes(1);

    if (typeof subscription === 'function') {
      subscription();
    }

    provider.updateLocation(createLocation('second'));
    expect(observer.onLocationUpdate).toHaveBeenCalledTimes(1);
  });
});

describe('FerrostarCore lifecycle', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('connects and disconnects location provider subscriptions', async () => {
    const locationProvider = new FakeLocationProvider();
    const core = new FerrostarCore(
      {} as any,
      locationProvider,
      createRouteProvider()
    );

    await core.connectLocationProvider(locationProvider);

    expect(locationProvider.observers.size).toBe(1);

    await core.disconnectLocationProvider();

    expect(locationProvider.observers.size).toBe(0);
    expect(locationProvider.unsubscribeCalls).toBe(1);
  });

  it('stopNavigation does not disconnect mounted provider subscription', async () => {
    mockSession();
    const locationProvider = new FakeLocationProvider();
    const core = new FerrostarCore(
      {} as any,
      locationProvider,
      createRouteProvider()
    );

    await core.connectLocationProvider(locationProvider);
    core.startNavigation(createRoute());
    core.stopNavigation();

    expect(locationProvider.observers.size).toBe(1);
  });

  it('updates last location before navigation and notifies listeners', async () => {
    const locationProvider = new FakeLocationProvider();
    const core = new FerrostarCore(
      {} as any,
      locationProvider,
      createRouteProvider()
    );
    const listener = vi.fn();

    core.addStateListener(listener);
    await core.connectLocationProvider(locationProvider);
    const location = createLocation();
    locationProvider.emitLocation(location);

    expect(core._lastLocation).toBe(location);
    expect(listener).toHaveBeenCalled();
  });

  it('startNavigation uses the latest provider location', async () => {
    const session = mockSession();
    const locationProvider = new FakeLocationProvider();
    const core = new FerrostarCore(
      {} as any,
      locationProvider,
      createRouteProvider()
    );
    const location = createLocation();

    await core.connectLocationProvider(locationProvider);
    locationProvider.emitLocation(location);
    core.startNavigation(createRoute());

    expect(session.getInitialState).toHaveBeenCalledWith(location);
  });

  it('notifies listeners with updated state for location updates', async () => {
    const session = mockSession();
    const locationProvider = new FakeLocationProvider();
    const core = new FerrostarCore(
      {} as any,
      locationProvider,
      createRouteProvider()
    );
    const listener = vi.fn();

    await core.connectLocationProvider(locationProvider);
    core.startNavigation(createRoute());
    core.addStateListener(listener);
    locationProvider.emitLocation(createLocation());

    const updatedState = session.updateUserLocation.mock.results[0]?.value;
    expect(listener).toHaveBeenLastCalledWith(core._state);
    expect(core._state.navState).toBe(updatedState);
    const lastCall = listener.mock.calls[listener.mock.calls.length - 1];
    expect(lastCall?.[0].navState).toBe(updatedState);
  });

  it('does not collide state listener ids after removals', () => {
    const core = new FerrostarCore(
      {} as any,
      new FakeLocationProvider(),
      createRouteProvider()
    );
    const first = vi.fn();
    const second = vi.fn();
    const third = vi.fn();

    const firstId = core.addStateListener(first);
    core.addStateListener(second);
    core.removeStateListener(firstId);
    core.addStateListener(third);

    core._state.set(createNavState('manual'), [], false);
    (core as any).notifyStateListeners();

    expect(first).not.toHaveBeenCalled();
    expect(second).toHaveBeenCalledTimes(1);
    expect(third).toHaveBeenCalledTimes(1);
  });

  it('transfers provider subscriptions', async () => {
    const firstProvider = new FakeLocationProvider();
    const secondProvider = new FakeLocationProvider();
    const secondLastLocation = createLocation();
    secondProvider.snapshot = { location: secondLastLocation };
    const core = new FerrostarCore(
      {} as any,
      firstProvider,
      createRouteProvider()
    );

    await core.connectLocationProvider(firstProvider);
    await core.connectLocationProvider(secondProvider);

    expect(firstProvider.observers.size).toBe(0);
    expect(secondProvider.observers.size).toBe(1);
    expect(core.locationProvider).toBe(secondProvider);
    expect(core._lastLocation).toBe(secondLastLocation);
  });

  it('async subscribe cleanup cannot attach a stale provider after provider change', async () => {
    const firstProvider = new AsyncFakeLocationProvider();
    const secondProvider = new FakeLocationProvider();
    const firstCleanup = vi.fn();
    const core = new FerrostarCore(
      {} as any,
      firstProvider,
      createRouteProvider()
    );

    const firstConnection = core.connectLocationProvider(firstProvider);
    await core.connectLocationProvider(secondProvider);
    firstProvider.resolveSubscribe?.(firstCleanup);
    await firstConnection;

    expect(firstCleanup).toHaveBeenCalledTimes(1);
    expect(core.locationProvider).toBe(secondProvider);
    expect(secondProvider.observers.size).toBe(1);
  });

  it('uses instance scoped navigation state', () => {
    const firstCore = new FerrostarCore(
      {} as any,
      new FakeLocationProvider(),
      createRouteProvider()
    );
    const secondCore = new FerrostarCore(
      {} as any,
      new FakeLocationProvider(),
      createRouteProvider()
    );

    expect(firstCore._state).not.toBe(secondCore._state);
  });
});
