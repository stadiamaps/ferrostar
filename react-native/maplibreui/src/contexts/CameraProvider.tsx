import { useRef, useState, type MutableRefObject } from 'react';
import {
  type CameraMode,
  type MapBounds,
  type MapPadding,
  NavigationActivity,
} from '../_types';
import {
  CameraRef,
  type ViewStateChangeEvent,
} from '@maplibre/maplibre-react-native';
import {
  useFerrostar,
  useNavigationState,
} from '@stadiamaps/ferrostar-core-react-native';
import { createContext } from 'react';
import { bbox } from '@turf/bbox';
import type { NativeSyntheticEvent } from 'react-native/types_generated/index';

export type CameraProviderContextType = {
  cameraMode: CameraMode;
  activity: NavigationActivity;
  location: [number, number] | null;
  zoom: number;
  pitch: number;
  muted: boolean;
  bounds: MapBounds | undefined;
  padding: MapPadding | undefined;
  setActivity: (activity: NavigationActivity) => void;
  setCameraMode: (mode: CameraMode) => void;
  cameraRef: MutableRefObject<CameraRef | null>;
  zoomIn: () => void;
  zoomOut: () => void;
  recenter: () => void;
  toggleMuted: () => void;
  overview: () => void;
  cameraChange: (e: NativeSyntheticEvent<ViewStateChangeEvent>) => void;
};

export const CameraProviderContext = createContext<
  CameraProviderContextType | undefined
>(undefined);

export type CameraProviderProps = {
  activity?: NavigationActivity;
  children: React.ReactNode;
};

export const CameraProvider = ({
  children,
  activity = NavigationActivity.Automotive,
}: CameraProviderProps) => {
  const cameraRef = useRef<CameraRef | null>(null);
  const core = useFerrostar();
  const { routeGeometry } = useNavigationState(core);
  const [cameraMode, setCameraMode] = useState<CameraMode>('following');
  const [zoom, setZoom] = useState<number>(activity.zoom);
  const [pitch, setPitch] = useState<number>(activity.pitch);
  const [muted, setMuted] = useState<boolean>(false);
  const [bounds, setBounds] = useState<MapBounds | undefined>(undefined);
  const [padding, setPadding] = useState<MapPadding | undefined>(undefined);
  const [activityState, setActivity] = useState<NavigationActivity>(activity);

  const zoomIn = () => {
    setZoom(zoom + 1);
  };

  const zoomOut = () => {
    setZoom(zoom - 1);
  };

  const recenter = () => {
    setZoom(activity.zoom);
    setPitch(activity.pitch);
    setBounds(null);
    setPadding(null);
    setCameraMode('following');
  };

  const toggleMuted = () => {
    setMuted(!muted);
    core.handleMuted(!muted);
  };

  const overview = () => {
    if (cameraMode === 'overview') {
      return;
    }

    if (routeGeometry === undefined || routeGeometry.length === 0) {
      return;
    }

    const lineString = {
      type: 'Feature',
      properties: {},
      geometry: {
        type: 'LineString',
        coordinates: routeGeometry.map((point) => [point.lng, point.lat]),
      },
    };
    const [west, south, east, north] = bbox(lineString);

    setCameraMode('overview');
    setPitch(0);
    setBounds([west, south, east, north]);
    setPadding({ top: 20, right: 20, bottom: 20, left: 20 });
  };

  const cameraChange = (event: NativeSyntheticEvent<ViewStateChangeEvent>) => {
    if (cameraMode === 'following' && event.nativeEvent.userInteraction) {
      setCameraMode('detached');
    }
  };

  return (
    <CameraProviderContext.Provider
      value={{
        cameraMode,
        setCameraMode,
        activity: activityState,
        setActivity,
        zoom,
        pitch,
        muted,
        bounds,
        padding,
        zoomIn,
        zoomOut,
        toggleMuted,
        recenter,
        overview,
        cameraChange,
        cameraRef,
      }}
    >
      {children}
    </CameraProviderContext.Provider>
  );
};
