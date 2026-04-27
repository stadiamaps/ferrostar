import { describe, expect, it, vi } from 'vitest';

vi.mock('@stadiamaps/ferrostar-uniffi-react-native', () => ({
  NavigationControllerConfig: class NavigationControllerConfig {},
  NavigationController: class NavigationController {},
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
  createNavigationSession: vi.fn(),
}));

import * as React from 'react';
import * as TestRenderer from 'react-test-renderer';
import type { FerrostarCore } from '../../FerrostarCore';
import type {
  LocationObserver,
  LocationProvider,
  LocationSubscription,
} from '../../LocationProvider';
import type { RouteProvider } from '../../RouteProvider';
import { FerrostarProvider } from '../../contexts/FerrostarProvider';
import { useFerrostar } from '../useFerrostar';

(
  globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }
).IS_REACT_ACT_ENVIRONMENT = true;

class FakeLocationProvider implements LocationProvider {
  observers = new Set<LocationObserver>();
  unsubscribeCalls = 0;

  subscribe(observer: LocationObserver): LocationSubscription {
    this.observers.add(observer);

    return () => {
      this.unsubscribeCalls += 1;
      this.observers.delete(observer);
    };
  }
}

type ProbeProps = {
  onCore: (core: FerrostarCore) => void;
};

function Probe({ onCore }: ProbeProps) {
  const core = useFerrostar();
  onCore(core);
  return null;
}

const createRouteProvider = (): RouteProvider => ({
  kind: 'custom',
  getRoutes: vi.fn(),
});

describe('useFerrostar', () => {
  it('returns the same core across input object identity changes', async () => {
    const firstConfig = { config: 1 } as any;
    const secondConfig = { config: 2 } as any;
    const firstRouteProvider = createRouteProvider();
    const secondRouteProvider = createRouteProvider();
    const cores: Array<FerrostarCore> = [];

    let renderer!: TestRenderer.ReactTestRenderer;
    await TestRenderer.act(async () => {
      renderer = TestRenderer.create(
        React.createElement(FerrostarProvider, {
          config: firstConfig,
          routeProvider: firstRouteProvider,
          children: React.createElement(Probe, {
            onCore: (core) => cores.push(core),
          }),
        } as any)
      );
    });

    const firstCore = cores[cores.length - 1];

    await TestRenderer.act(async () => {
      renderer.update(
        React.createElement(FerrostarProvider, {
          config: secondConfig,
          routeProvider: secondRouteProvider,
          children: React.createElement(Probe, {
            onCore: (core) => cores.push(core),
          }),
        } as any)
      );
    });

    const secondCore = cores[cores.length - 1];

    expect(secondCore).toBe(firstCore);
    expect(secondCore?.navigationControllerConfig).toBe(secondConfig);
    expect(secondCore?.routeProvider).toBe(secondRouteProvider);
  });

  it('transfers provider subscription when the provider prop changes', async () => {
    const firstProvider = new FakeLocationProvider();
    const secondProvider = new FakeLocationProvider();
    const routeProvider = createRouteProvider();
    const cores: Array<FerrostarCore> = [];

    let renderer!: TestRenderer.ReactTestRenderer;
    await TestRenderer.act(async () => {
      renderer = TestRenderer.create(
        React.createElement(FerrostarProvider, {
          config: {} as any,
          routeProvider,
          locationProvider: firstProvider,
          children: React.createElement(Probe, {
            onCore: (core) => cores.push(core),
          }),
        } as any)
      );
    });

    const firstCore = cores[cores.length - 1];
    expect(firstProvider.observers.size).toBe(1);

    await TestRenderer.act(async () => {
      renderer.update(
        React.createElement(FerrostarProvider, {
          config: {} as any,
          routeProvider,
          locationProvider: secondProvider,
          children: React.createElement(Probe, {
            onCore: (core) => cores.push(core),
          }),
        } as any)
      );
    });

    const secondCore = cores[cores.length - 1];

    expect(secondCore).toBe(firstCore);
    expect(secondCore?.locationProvider).toBe(secondProvider);
    expect(firstProvider.observers.size).toBe(0);
    expect(firstProvider.unsubscribeCalls).toBe(1);
    expect(secondProvider.observers.size).toBe(1);
  });

  it('disconnects provider subscription on unmount', async () => {
    const provider = new FakeLocationProvider();
    const routeProvider = createRouteProvider();

    let renderer!: TestRenderer.ReactTestRenderer;
    await TestRenderer.act(async () => {
      renderer = TestRenderer.create(
        React.createElement(FerrostarProvider, {
          config: {} as any,
          routeProvider,
          locationProvider: provider,
          children: null,
        } as any)
      );
    });

    expect(provider.observers.size).toBe(1);

    await TestRenderer.act(async () => {
      renderer.unmount();
    });

    expect(provider.observers.size).toBe(0);
    expect(provider.unsubscribeCalls).toBe(1);
  });
});
