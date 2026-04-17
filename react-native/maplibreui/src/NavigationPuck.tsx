import { useMemo, useState } from 'react';
import { GeoJSONSource, Layer, Images } from '@maplibre/maplibre-react-native';
import {
  useNavigationState,
  useFerrostar,
} from '@stadiamaps/ferrostar-core-react-native';
import arrowIcon from '../assets/navigation_puck.png';

interface NavigationPuckProps {
  /**
   * The size of the puck in points.
   * @default 40
   */
  size?: number;
}

/**
 * A custom navigation puck that displays the user's location and heading using map layers.
 *
 * This puck uses CircleLayer and SymbolLayer for high performance.
 */
export const NavigationPuck = ({ size = 40 }: NavigationPuckProps) => {
  const core = useFerrostar();
  const { location } = useNavigationState(core);
  const [images, setImages] = useState({
    'ferrostar-puck-arrow': arrowIcon,
  });

  const geojson = useMemo(() => {
    if (!location) return undefined;
    return {
      type: 'FeatureCollection' as const,
      features: [
        {
          type: 'Feature' as const,
          geometry: {
            type: 'Point' as const,
            coordinates: [location.coordinates.lng, location.coordinates.lat],
          },
          properties: {
            heading: location.courseOverGround?.degrees ?? 0,
          },
        },
      ],
    };
  }, [location]);

  if (!geojson) {
    return null;
  }

  return (
    <>
      <Images
        images={images}
        onImageMissing={(imageKey) =>
          setImages((prevState) => ({
            ...prevState,
            [imageKey]: arrowIcon,
          }))
        }
      />
      <GeoJSONSource id="ferrostar-puck-source" data={geojson}>
        {/* White Circle Background */}
        <Layer
          id="ferrostar-puck-bg"
          beforeId="ferrostar-puck-arrow-layer"
          type="circle"
          paint={{
            circleRadius: size / 2,
            circleColor: 'white',
            circleStrokeWidth: 1,
            circleStrokeColor: 'rgba(0,0,0,0.1)',
            circlePitchAlignment: 'map',
          }}
        />

        {/* Blue Arrow Group rotated by heading */}
        <Layer
          id="ferrostar-puck-arrow-layer"
          type="symbol"
          layout={{
            iconImage: 'ferrostar-puck-arrow',
            iconRotate: ['get', 'heading'],
            iconRotationAlignment: 'map',
            iconAllowOverlap: true,
            iconIgnorePlacement: true,
            iconSize: size / 180,
            iconPitchAlignment: 'map',
          }}
        />
      </GeoJSONSource>
    </>
  );
};
