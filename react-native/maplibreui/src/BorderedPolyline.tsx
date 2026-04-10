import { Layer, GeoJSONSource } from '@maplibre/maplibre-react-native';

type BorderedPolylineProps = {
  points: Array<{ lat: number; lng: number }>;
  zIndex?: number;
  color?: string;
  borderColor?: string;
  lineWidth?: number;
  borderWidth?: number;
};

export const BorderedPolyline = ({
  points,
  zIndex = 1,
  color = '#3583dd',
  borderColor = '#ffffff',
  lineWidth = 10.0,
  borderWidth = 3.0,
}: BorderedPolylineProps) => {
  if (points.length < 2) {
    return null;
  }

  return (
    <GeoJSONSource
      id="border-polyline"
      data={{
        type: 'Feature',
        geometry: {
          type: 'LineString',
          coordinates: points.map((p) => [p.lng, p.lat]) as number[][],
        },
        properties: {},
      }}
    >
      <Layer
        id="line-border"
        type="line"
        beforeId="line"
        style={{
          lineCap: 'round',
          lineJoin: 'round',
          lineWidth: lineWidth + borderWidth * 2.0,
          lineColor: borderColor,
          lineSortKey: zIndex,
        }}
      />
      <Layer
        id="line"
        type="line"
        beforeId="ferrostar-puck-bg"
        style={{
          lineCap: 'round',
          lineJoin: 'round',
          lineWidth,
          lineColor: color,
          lineSortKey: zIndex,
        }}
      />
    </GeoJSONSource>
  );
};
