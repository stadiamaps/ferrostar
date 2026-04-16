import { Camera, type CameraRef } from '@maplibre/maplibre-react-native';
import {
  forwardRef,
  useEffect,
  useImperativeHandle,
  useMemo,
  useRef,
} from 'react';
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
    { activity = NavigationActivity.Automotive, followUserLocation },
    outerRef
  ) => {
    const innerRef = useRef<CameraRef>(null);

    useImperativeHandle<CameraRef | null, CameraRef | null>(
      outerRef,
      () => innerRef.current
    );

    const centerCoordinate: [number, number] | undefined = useMemo(() => {
      if (!followUserLocation) return undefined;
      return [
        followUserLocation.coordinates.lng,
        followUserLocation.coordinates.lat,
      ];
    }, [followUserLocation]);

    const heading = useMemo(() => {
      return followUserLocation?.courseOverGround?.degrees ?? 0;
    }, [followUserLocation]);

    useEffect(() => {
      if (centerCoordinate) {
        innerRef.current?.easeTo(
          {
            center: centerCoordinate,
            bearing: heading,
          },
          50
        );
      }
    }, [centerCoordinate, heading]);

    return (
      <Camera ref={innerRef} zoomLevel={activity.zoom} pitch={activity.pitch} />
    );
  }
);
