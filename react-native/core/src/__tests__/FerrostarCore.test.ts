import { beforeEach, describe, expect, expectTypeOf, it, vi } from 'vitest';

vi.mock('@stadiamaps/ferrostar-uniffi-react-native', () => ({
  createNavigationSession: vi.fn(),
  NavigationControllerConfig: class NavigationControllerConfig {},
  NavigationController: class NavigationController {},
  NavigationRecorder: class NavigationRecorder {
    getEvents(): Array<unknown> {
      return [];
    }

    getRecordingJson(): string {
      return '{"events":[]}';
    }
  },
  LocationBias: {
    None: class None {},
  },
  RouteAdapter: {
    fromWellKnownRouteProvider: vi.fn(),
  },
  RouteDeviation: {
    Deviation: {
      instanceOf: (value: { tag?: string } | undefined) =>
        value?.tag === 'Deviation',
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

import {
  createNavigationSession,
  NavigationRecorder,
  type NavigationControllerConfig,
  type NavState,
  type Route,
  type UserLocation,
} from '@stadiamaps/ferrostar-uniffi-react-native';
import { FerrostarCore } from '../FerrostarCore';
import type {
  ForegroundService,
  ForegroundServiceStartOptions,
} from '../ForegroundService';
import {
  ManualLocationProvider,
  SimulatedLocationProvider,
  type LocationObserver,
  type LocationProvider,
  type LocationSnapshot,
  type LocationSubscription,
} from '../LocationProvider';
import type { RouteProvider } from '../RouteProvider';
import type { NavigationRecording } from '../NavigationRecording';

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

  emitLocation(location: UserLocation): void {
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

class FakeForegroundService implements ForegroundService {
  events: Array<string> = [];
  startOptions?: ForegroundServiceStartOptions;
  states: Array<unknown> = [];

  start(options: ForegroundServiceStartOptions): void {
    this.events.push('start');
    this.startOptions = options;
  }

  update(state: unknown): void {
    this.events.push('update');
    this.states.push(state);
  }

  stop(): void {
    this.events.push('stop');
  }
}

const flushForegroundServiceOperations = async (): Promise<void> => {
  for (let operation = 0; operation < 10; operation += 1) {
    await Promise.resolve();
  }
};

const createRouteProvider = (): RouteProvider => ({
  kind: 'custom',
  getRoutes: vi.fn(),
});

const createRoute = () =>
  ({
    geometry: [{ lat: 1, lng: 2 }],
  }) as Route;

const createLocation = (id = 'location') =>
  ({
    id,
    coordinates: { lat: 1, lng: 2 },
    horizontalAccuracy: 5,
    timestamp: new Date(0),
  }) as UserLocation;

const createNavState = (id: string) =>
  ({
    id,
    tripState: {
      tag: 'Navigating',
      inner: {
        deviation: { tag: 'NoDeviation' },
        remainingWaypoints: [],
      },
    },
  }) as NavState;

const createConfig = (id?: string) =>
  ({ id }) as unknown as NavigationControllerConfig;

const mockSession = (initialState = createNavState('initial')) => {
  const session = {
    getInitialState: vi.fn(() => initialState),
    updateUserLocation: vi.fn(() => createNavState('updated')),
    advanceToNextStep: vi.fn(),
  };

  vi.mocked(createNavigationSession).mockReturnValue(
    session as unknown as ReturnType<typeof createNavigationSession>
  );
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
      createConfig(),
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
      createConfig(),
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
      createConfig(),
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
      createConfig(),
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
      createConfig(),
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
      createConfig(),
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
    core.onLocationUpdate(createLocation());

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
      createConfig(),
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
      createConfig(),
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
      createConfig(),
      new FakeLocationProvider(),
      createRouteProvider()
    );
    const secondCore = new FerrostarCore(
      createConfig(),
      new FakeLocationProvider(),
      createRouteProvider()
    );

    expect(firstCore._state).not.toBe(secondCore._state);
  });

  it('attaches a recorder when recording is explicitly enabled', () => {
    mockSession();
    const route = createRoute();
    const config = createConfig('recording-config');
    const core = new FerrostarCore(
      createConfig(),
      new FakeLocationProvider(),
      createRouteProvider()
    );

    const recording = core.startNavigation(route, {
      recording: true,
      config,
    });

    expectTypeOf(recording).toMatchTypeOf<NavigationRecording>();
    expect(recording).toBeInstanceOf(NavigationRecorder);
    expect(recording.getRecordingJson()).toBe('{"events":[]}');
    expect(createNavigationSession).toHaveBeenLastCalledWith(route, config, [
      recording,
    ]);
  });

  it('keeps the active recorder when replacing a route', () => {
    mockSession();
    const initialRoute = createRoute();
    const replacementRoute = createRoute();
    const initialConfig = createConfig('initial-config');
    const replacementConfig = createConfig('replacement-config');
    const core = new FerrostarCore(
      createConfig(),
      new FakeLocationProvider(),
      createRouteProvider()
    );

    const recording = core.startNavigation(initialRoute, {
      recording: true,
      config: initialConfig,
    });
    core.replaceRoute(replacementRoute, replacementConfig);

    expect(createNavigationSession).toHaveBeenLastCalledWith(
      replacementRoute,
      replacementConfig,
      [recording]
    );
  });

  it('does not reuse a stopped recorder for a later session', () => {
    mockSession();
    const route = createRoute();
    const config = createConfig('config');
    const core = new FerrostarCore(
      createConfig(),
      new FakeLocationProvider(),
      createRouteProvider()
    );

    const recording = core.startNavigation(route, {
      recording: true,
      config,
    });
    core.stopNavigation();
    core.startNavigation(route, config);

    expect(recording.getRecordingJson()).toBe('{"events":[]}');
    expect(createNavigationSession).toHaveBeenLastCalledWith(route, config, []);
  });

  it('leaves recording serialization errors with the caller', () => {
    mockSession();
    const defaultConfig = createConfig('default-config');
    const route = createRoute();
    const core = new FerrostarCore(
      defaultConfig,
      new FakeLocationProvider(),
      createRouteProvider()
    );
    const recording = core.startNavigation(route, {
      recording: true,
    });
    expect(createNavigationSession).toHaveBeenLastCalledWith(
      route,
      defaultConfig,
      [recording]
    );
    const expected = new Error('serialization failed');
    vi.spyOn(recording, 'getRecordingJson').mockImplementation(() => {
      throw expected;
    });

    expect(() => recording.getRecordingJson()).toThrow(expected);
  });

  it('drives the foreground service for the navigation lifecycle', async () => {
    mockSession();
    const foregroundService = new FakeForegroundService();
    const core = new FerrostarCore(
      createConfig(),
      new FakeLocationProvider(),
      createRouteProvider(),
      undefined,
      undefined,
      foregroundService
    );

    core.startNavigation(createRoute());
    await flushForegroundServiceOperations();

    expect(foregroundService.events).toEqual(['start', 'update']);
    expect(foregroundService.states[0]).not.toBe(core._state);
    expect(
      (foregroundService.states[0] as { navState?: unknown }).navState
    ).toBe(core._state.navState);

    foregroundService.startOptions?.stopNavigation();
    await flushForegroundServiceOperations();

    expect(foregroundService.events).toEqual(['start', 'update', 'stop']);
    expect(core._state.isNavigating()).toBe(false);
  });

  it('waits for foreground service start before sending updates', async () => {
    mockSession();
    let finishStarting: (() => void) | undefined;
    const foregroundService: ForegroundService = {
      start: vi.fn(
        () =>
          new Promise<void>((resolve) => {
            finishStarting = resolve;
          })
      ),
      update: vi.fn(),
      stop: vi.fn(),
    };
    const core = new FerrostarCore(
      createConfig(),
      new FakeLocationProvider(),
      createRouteProvider(),
      undefined,
      undefined,
      foregroundService
    );

    core.startNavigation(createRoute());
    await Promise.resolve();

    expect(foregroundService.start).toHaveBeenCalledTimes(1);
    expect(foregroundService.update).not.toHaveBeenCalled();

    finishStarting?.();
    await flushForegroundServiceOperations();

    expect(foregroundService.update).toHaveBeenCalledTimes(1);
  });

  it('does not restart the foreground service when replacing a route', async () => {
    mockSession();
    const foregroundService = new FakeForegroundService();
    const core = new FerrostarCore(
      createConfig(),
      new FakeLocationProvider(),
      createRouteProvider(),
      undefined,
      undefined,
      foregroundService
    );

    core.startNavigation(createRoute());
    core.replaceRoute(createRoute());
    await flushForegroundServiceOperations();

    expect(
      foregroundService.events.filter((event) => event === 'start')
    ).toHaveLength(1);
    expect(
      foregroundService.events.filter((event) => event === 'update')
    ).toHaveLength(2);
  });

  it('continues foreground cleanup after an adapter failure', async () => {
    mockSession();
    const expectedError = new Error('could not start');
    const foregroundService: ForegroundService = {
      start: vi.fn(() => {
        throw expectedError;
      }),
      update: vi.fn(),
      stop: vi.fn(),
    };
    const consoleError = vi
      .spyOn(console, 'error')
      .mockImplementation(() => undefined);
    const core = new FerrostarCore(
      createConfig(),
      new FakeLocationProvider(),
      createRouteProvider(),
      undefined,
      undefined,
      foregroundService
    );

    core.startNavigation(createRoute());
    core.stopNavigation();
    await flushForegroundServiceOperations();

    expect(consoleError).toHaveBeenCalledWith(
      'Foreground service start failed:',
      expectedError
    );
    expect(foregroundService.stop).toHaveBeenCalledTimes(1);

    consoleError.mockRestore();
  });
});
