import type { GeocodingGeoJSONProperties } from '@stadiamaps/api';
import LocationOn from './icons/location-on';
import LocationCity from './icons/location-city';
import Address from './icons/123';
import PostBox from './icons/post-box';
import Road from './icons/road';
import Water from './icons/water';
import Globe from './icons/globe';
import GlobeLines from './icons/globe-lines';
import type { ReactElement } from 'react';
import { type Position } from 'geojson';
import distance from '@turf/distance';

export function subtitle(properties: GeocodingGeoJSONProperties): string {
  let components: (string | undefined)[] = [];
  switch (properties.layer) {
    case 'venue':
    case 'address':
    case 'street':
    case 'neighbourhood':
    case 'postalcode':
    case 'macrohood':
      components = [
        properties.locality ?? properties.region,
        properties.country,
      ];
      break;
    case 'country':
    case 'dependency':
    case 'disputed':
      components = [properties.continent];
      break;
    case 'macroregion':
    case 'region':
      components = [properties.country];
      break;
    case 'macrocounty':
    case 'county':
    case 'locality':
    case 'localadmin':
    case 'borough':
      components = [properties.region, properties.country];
      break;
    case 'coarse':
    case 'marinearea':
    case 'empire':
    case 'continent':
    case 'ocean':
      break;
  }

  return components.filter((x) => x !== null && x !== undefined).join(', ');
}

type Icon = ReactElement | null;

export function icon(layer: string): Icon {
  switch (layer) {
    case 'venue':
      return <LocationOn />;
    case 'address':
      return <Address />;
    case 'street':
      return <Road />;
    case 'postalcode':
      return <PostBox />;
    case 'localadmin':
      return null;
    case 'locality':
      return null;
    case 'borough':
      return null;
    case 'neighbourhood':
      return null;
    case 'macrohood':
      return null;
    case 'coarse': // Never actually encountered
      return <LocationCity />;
    case 'county':
      return null;
    case 'macroregion':
      return null;
    case 'macrocounty':
      return null;
    // The regions above this line which are bigger than a "city"
    // but smaller than a "state" or similar unit
    // could do with a better icon, but finding one has proven elusive
    case 'region':
      return null;
    case 'country':
      return null;
    case 'dependency':
      return null;
    case 'disputed':
      return <Globe />;
    case 'empire':
      return null;
    case 'continent':
      return <GlobeLines />;
    case 'marinearea':
      return null;
    case 'ocean':
      return <Water />;
    default:
      return null;
  }
}

export function distanceSubtitle(
  point: Position,
  userLocation: { lat: number; lng: number }
): string {
  const distanceBetween = distance(
    point,
    [userLocation.lng, userLocation.lat],
    { units: 'kilometres' }
  );

  return `${distanceBetween.toFixed(2)} km`;
}
