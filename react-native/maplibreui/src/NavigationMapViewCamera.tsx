import {
  Camera,
  type CameraRef,
  type ViewPadding,
} from '@maplibre/maplibre-react-native';
import {
  forwardRef,
  useEffect,
  useImperativeHandle,
  useMemo,
  useRef,
} from 'react';
import { Dimensions, PixelRatio, useWindowDimensions } from 'react-native';
import type { UserLocation } from '@stadiamaps/ferrostar-core-react-native';

export class NavigationActivity {
  zoom: number;
  pitch: number;

  constructor(zoom: number, pitch: number) {
    this.zoom = zoom;
    this.pitch = pitch;
  }

  /* The recommended camera configuration for automotive navigation. */
  static Automotive = new NavigationActivity(16.0, 45.0);

  /* The recommended camera configuration for bicycle navigation. */
  static Bicycle = new NavigationActivity(18.0, 45.0);

  /* The recommended camera configuration for pedestrian navigation. */
  static Pedestrian = new NavigationActivity(20.0, 10.0);
}

type NavigationMapViewCameraProps = {
  activity?: NavigationActivity;
  bounds?: { ne: [number, number]; sw: [number, number] } | null;
  followUserLocation?: UserLocation;
};

/**
 * The camera configuration for navigation. This configuration sets the camera to track the user,
 * with a high zoom level and moderate pitch for a 2.5D isometric view. It automatically adjusts the
 * padding based on the screen size and orientation.
 *
 * @param activity The type of activity the camera is being used for.
 * @return The recommended navigation MapViewCamera
 */
export const NavigationMapViewCamera = forwardRef<
  CameraRef,
  NavigationMapViewCameraProps
>(
  (
    {
      activity = NavigationActivity.Automotive,
      bounds = null,
      followUserLocation,
    },
    outerRef
  ) => {
    const innerRef = useRef<CameraRef>(null);

    useImperativeHandle<CameraRef | null, CameraRef | null>(
      outerRef,
      () => innerRef.current
    );

    const { width, height } = useWindowDimensions();
    const orientation = useMemo(() => {
      return height > width ? 'portrait' : 'landscape';
    }, [height, width]);

    const start = useMemo(() => {
      if (orientation === 'landscape') return 0.5;
      return 0.0;
    }, [orientation]);

    const padding: ViewPadding = useMemo(() => {
      const { height: screenHeight, width: screenWidth } =
        Dimensions.get('screen');

      const screenWidthPx = PixelRatio.getPixelSizeForLayoutSize(screenWidth);
      const screenHeightPx = PixelRatio.getPixelSizeForLayoutSize(screenHeight);
      // TODO: A way to calculate RTL and LTR padding.
      const left = start * screenWidthPx;
      const top = 0.5 * screenHeightPx;
      const right = 0.0 * screenWidthPx;
      const bottom = 0.0 * screenHeightPx;

      return {
        left,
        top,
        right,
        bottom,
      };
    }, [start]);

    const centerCoordinate = useMemo(() => {
      if (!followUserLocation) return undefined;
      return [
        followUserLocation.coordinates.lng,
        followUserLocation.coordinates.lat,
      ] as [number, number];
    }, [followUserLocation]);

    const heading = useMemo(() => {
      return followUserLocation?.courseOverGround?.degrees ?? 0;
    }, [followUserLocation]);

    useEffect(() => {
      innerRef.current.easeTo(
        { center: centerCoordinate, bearing: heading },
        50
      );
    }, [centerCoordinate]);

    return (
      <Camera
        ref={innerRef}
        trackUserLocation={followUserLocation ? undefined : 'course'}
        zoom={activity.zoom}
        pitch={activity.pitch}
        padding={padding}
      />
    );
  }
);
