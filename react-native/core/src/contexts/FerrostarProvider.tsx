import { useMemo } from 'react';
import { FerrostarCore } from '../FerrostarCore';
import {
  ManualLocationProvider,
  type LocationProviderInterface,
} from '../LocationProvider';
import { NavigationControllerConfig } from '@stadiamaps/ferrostar-uniffi-react-native';
import { createContext } from 'react';

type FerrostarProviderContextType = {
  core: FerrostarCore;
};

export const FerrostarContext = createContext<
  FerrostarProviderContextType | undefined
>(undefined);

type FerrostarProviderProps = {
  config: NavigationControllerConfig;
  routeProvider: RouteProvider;
  locationProvider?: LocationProviderInterface;
  children: React.ReactNode;
};

export const FerrostarProvider = ({
  config,
  routeProvider,
  locationProvider,
  children,
}: FerrostarProviderProps) => {
  const core = useMemo(() => {
    return new FerrostarCore(
      config,
      locationProvider ?? new ManualLocationProvider(),
      routeProvider
    );
  }, [config, routeProvider, locationProvider]);

  const addLocationListener = (listener: (location: UserLocation) => void) => {
    return core.locationProvider.addListener(listener);
  };
  const removeLocationListener = (id: number) => {
    core.locationProvider.removeListener(id);
  };

  return (
    <FerrostarContext.Provider
      value={{ core, addLocationListener, removeLocationListener }}
    >
      {children}
    </FerrostarContext.Provider>
  );
};
