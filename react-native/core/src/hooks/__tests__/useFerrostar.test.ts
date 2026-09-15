import { describe, expect, it, vi } from 'vitest';

vi.mock('@stadiamaps/ferrostar-uniffi-react-native', () => ({
  DrivingSide: { Right: 'Right' },
  NavigationControllerConfig: class NavigationControllerConfig {},
  NavigationController: class NavigationController {},
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
  createNavigationSession: vi.fn(),
}));

import * as React from 'react';
import * as TestRenderer from 'react-test-renderer';
import type { NavigationControllerConfig } from '@stadiamaps/ferrostar-uniffi-react-native';
import { FerrostarCore } from '../../FerrostarCore';
import type {
  LocationObserver,
  LocationProvider,
  LocationSubscription,
} from '../../LocationProvider';
import type { RouteProvider } from '../../RouteProvider';
import { FerrostarProvider } from '../../contexts/FerrostarProvider';
import { useFerrostar } from '../useFerrostar';
import { useNavigationState } from '../useNavigationState';

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

type NavigationStateProbeProps = {
  core: FerrostarCore;
  onMutedChange: (isMuted: boolean | undefined) => void;
};

function NavigationStateProbe({
  core,
  onMutedChange,
}: NavigationStateProbeProps) {
  const state = useNavigationState(core);
  onMutedChange(state.isMuted);
  return null;
}

const createRouteProvider = (): RouteProvider => ({
  kind: 'custom',
  getRoutes: vi.fn(),
});

const createConfig = (id?: number) =>
  ({ id }) as unknown as NavigationControllerConfig;

describe('useFerrostar', () => {
  it('returns the same core across input object identity changes', async () => {
    const firstConfig = createConfig(1);
    const secondConfig = createConfig(2);
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
        })
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
        })
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
          config: createConfig(),
          routeProvider,
          locationProvider: firstProvider,
          children: React.createElement(Probe, {
            onCore: (core) => cores.push(core),
          }),
        })
      );
    });

    const firstCore = cores[cores.length - 1];
    expect(firstProvider.observers.size).toBe(1);

    await TestRenderer.act(async () => {
      renderer.update(
        React.createElement(FerrostarProvider, {
          config: createConfig(),
          routeProvider,
          locationProvider: secondProvider,
          children: React.createElement(Probe, {
            onCore: (core) => cores.push(core),
          }),
        })
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

    const cores: Array<FerrostarCore> = [];
    let renderer!: TestRenderer.ReactTestRenderer;
    await TestRenderer.act(async () => {
      renderer = TestRenderer.create(
        React.createElement(FerrostarProvider, {
          config: createConfig(),
          routeProvider,
          locationProvider: provider,
          children: React.createElement(Probe, {
            onCore: (core) => cores.push(core),
          }),
        })
      );
    });

    expect(provider.observers.size).toBe(1);
    const core = cores[cores.length - 1];
    if (!core) {
      throw new Error('Expected FerrostarProvider to create a core');
    }
    const stopNavigation = vi.spyOn(core, 'stopNavigation');

    await TestRenderer.act(async () => {
      renderer.unmount();
    });

    expect(provider.observers.size).toBe(0);
    expect(provider.unsubscribeCalls).toBe(1);
    expect(stopNavigation).toHaveBeenCalledTimes(1);
  });

  it('updates navigation state subscribers when mute changes', async () => {
    const core = new FerrostarCore(
      createConfig(),
      new FakeLocationProvider(),
      createRouteProvider()
    );
    const mutedStates: Array<boolean | undefined> = [];

    let renderer!: TestRenderer.ReactTestRenderer;
    await TestRenderer.act(async () => {
      renderer = TestRenderer.create(
        React.createElement(NavigationStateProbe, {
          core,
          onMutedChange: (isMuted) => mutedStates.push(isMuted),
        })
      );
    });

    await TestRenderer.act(async () => {
      core.handleMuted(true);
    });

    expect(mutedStates[mutedStates.length - 1]).toBe(true);

    await TestRenderer.act(async () => {
      renderer.unmount();
    });
    expect(core._listeners.size).toBe(0);
  });
});
