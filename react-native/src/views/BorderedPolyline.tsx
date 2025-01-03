import { LineLayer, ShapeSource } from '@maplibre/maplibre-react-native';

type BorderedPolylineProps = {
  points: Array<{ lat: number; lng: number }>;
  zIndex?: number;
  color?: string;
  borderColor?: string;
  lineWidth?: number;
  borderWidth?: number;
};

const BorderedPolyline = ({
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
    <ShapeSource
      id="border-polyline"
      shape={{
        type: 'Feature',
        geometry: {
          type: 'LineString',
          coordinates: points.map((p) => [p.lng, p.lat]) as number[][],
        },
        properties: {},
      }}
    >
      <LineLayer
        id="line-border"
        style={{
          lineCap: 'round',
          lineWidth: lineWidth + borderWidth * 2.0,
          lineColor: borderColor,
          lineSortKey: zIndex,
        }}
      />
      <LineLayer
        id="line"
        style={{
          lineCap: 'round',
          lineWidth,
          lineColor: color,
          lineSortKey: zIndex,
        }}
      />
    </ShapeSource>
  );
};

export default BorderedPolyline;
