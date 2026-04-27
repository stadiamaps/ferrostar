import { createContext, useEffect, useRef, type ReactNode } from 'react';
import { FerrostarCore } from '../FerrostarCore';
import {
  ManualLocationProvider,
  type LocationProvider,
} from '../LocationProvider';
import { ManualSpeechEngine, type SpeechEngine } from '../SpeechEngine';
import { NavigationControllerConfig } from '@stadiamaps/ferrostar-uniffi-react-native';
import type { RouteDeviationHandler } from '../RouteDeviationHandler';
import type { RouteProvider } from '../RouteProvider';

type FerrostarProviderContextType = {
  core: FerrostarCore;
};

export const FerrostarContext = createContext<
  FerrostarProviderContextType | undefined
>(undefined);

type FerrostarProviderProps = {
  config: NavigationControllerConfig;
  routeProvider: RouteProvider;
  locationProvider?: LocationProvider;
  speechEngine?: SpeechEngine;
  deviationHandler?: RouteDeviationHandler;
  children: ReactNode;
};

export const FerrostarProvider = ({
  config,
  routeProvider,
  locationProvider,
  speechEngine,
  deviationHandler,
  children,
}: FerrostarProviderProps) => {
  const fallbackLocationProviderRef = useRef<LocationProvider>();
  if (!fallbackLocationProviderRef.current) {
    fallbackLocationProviderRef.current = new ManualLocationProvider();
  }

  const effectiveLocationProvider =
    locationProvider ?? fallbackLocationProviderRef.current;
  const effectiveSpeechEngine = speechEngine ?? ManualSpeechEngine;

  const coreRef = useRef<FerrostarCore>();
  if (!coreRef.current) {
    coreRef.current = new FerrostarCore(
      config,
      effectiveLocationProvider,
      routeProvider,
      effectiveSpeechEngine,
      deviationHandler
    );
  }

  const core = coreRef.current;
  core.navigationControllerConfig = config;
  core.routeProvider = routeProvider;
  core.speechEngine = effectiveSpeechEngine;
  core.deviationHandler = deviationHandler;

  useEffect(() => {
    void core.connectLocationProvider(effectiveLocationProvider);

    return () => {
      void core.disconnectLocationProvider();
    };
  }, [core, effectiveLocationProvider]);

  return (
    <FerrostarContext.Provider value={{ core }}>
      {children}
    </FerrostarContext.Provider>
  );
};
