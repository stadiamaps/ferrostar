import { Camera } from '@maplibre/maplibre-react-native';
import {
  useFerrostar,
  useNavigationState,
} from '@stadiamaps/ferrostar-core-react-native';
import { useCamera } from './hooks/useCamera';

/**
 * The camera configuration for navigation. This configuration sets the camera to track the user,
 * with a high zoom level and moderate pitch for a 2.5D isometric view. It automatically adjusts the
 * padding based on the screen size and orientation.
 *
 * @return The recommended navigation MapViewCamera
 */
export const NavigationCamera = () => {
  const core = useFerrostar();
  const { location } = useNavigationState(core);
  const { cameraRef, cameraMode, zoom, pitch, bounds, padding } = useCamera();

  const { lng, lat } = location.coordinates;
  const bearing = location.courseOverGround?.degrees ?? 0;

  return (
    <Camera
      ref={cameraRef}
      center={cameraMode === 'following' ? [lng, lat] : undefined}
      zoom={zoom}
      pitch={pitch}
      bearing={cameraMode === 'following' ? bearing : 0}
      bounds={bounds}
      padding={padding}
      easing="ease"
      duration={300}
    />
  );
};
