import MapLibreGL, { MapView } from '@maplibre/maplibre-react-native';
import { useEffect, useState, type ComponentProps } from 'react';
import { FerrostarCore, type NavigationState } from '../core/FerrostarCore';

MapLibreGL.setAccessToken(null);

type NavigationViewProps = ComponentProps<typeof MapView> & {
  core: FerrostarCore;
};

const NavigationView = (props: NavigationViewProps) => {
  const { core } = props;
  const [navigationState, setNavigationState] = useState<NavigationState>();

  useEffect(() => {
    const watchId = core.addStateListener((state) => {
      setNavigationState(state);
    });

    return () => {
      core.removeStateListener(watchId);
    };
  }, [core]);

  return <MapView {...props} />;
};

export default NavigationView;
