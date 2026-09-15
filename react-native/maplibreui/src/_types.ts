export type CameraMode = 'following' | 'overview' | 'detached';

export type MapBounds = [number, number, number, number];

export type MapPadding = {
  top: number;
  right: number;
  bottom: number;
  left: number;
};

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
