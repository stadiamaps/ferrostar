import {
  AutocompleteV2Request,
  Configuration,
  FeaturePropertiesV2,
  GeocodingApi,
  GeocodingLayer,
  SearchRequest,
  type GeocodingGeoJSONFeature,
} from '@stadiamaps/api';
import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Keyboard,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
  type StyleProp,
  type ViewStyle,
} from 'react-native';
import { distanceSubtitle, icon } from './_utils';
import { useDebounce } from './use-debounce';
import { TextStyle } from 'react-native/Libraries/StyleSheet/StyleSheetTypes';

type AutocompleteSearchContextType = {
  api: GeocodingApi;
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  result: FeaturePropertiesV2 | null;
  setResult: (result: FeaturePropertiesV2 | null) => void;
  results: FeaturePropertiesV2[];
  isLoading: boolean;
  config: Configuration;
  userLocation: { lat: number; lng: number };
  limitLayers?: GeocodingLayer[];
  maxResults?: number;
  onResultSelected?: (result: any) => void;
};

const AutocompleteSearchContext = createContext<AutocompleteSearchContextType>({
  api: new GeocodingApi(),
  searchQuery: '',
  setSearchQuery: () => {},
  result: null,
  setResult: () => {},
  results: [],
  isLoading: false,
  config: new Configuration(),
  userLocation: { lat: 0, lng: 0 },
  limitLayers: undefined,
  maxResults: undefined,
  onResultSelected: () => {},
});

type AutocompleteSearchRootProps = {
  config: Configuration;
  userLocation: { lat: number; lng: number };
  limitLayers?: GeocodingLayer[];
  minimumSearchLength?: number;
  onResultSelected?: (result: FeaturePropertiesV2 | null) => void;
  style?: StyleProp<ViewStyle>;
  children: React.ReactNode;
};

const search = async (
  api: GeocodingApi,
  searchQuery: string,
  userLocation: { lat: number; lng: number },
  maxResults?: number
) => {
  const request: AutocompleteV2Request = {
    text: searchQuery,
    focusPointLat: userLocation.lat,
    focusPointLon: userLocation.lng,
    //boundaryCircleLat: userLocation.lat,
    //boundaryCircleLon: userLocation.lng,
    //boundaryCircleRadius: 400,
    size: maxResults,
  };
  const { features } = await api.autocompleteV2(request);

  return features;
};

const AutocompleteSearchRootBase = (props: AutocompleteSearchRootProps) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [features, setFeatures] = useState<FeaturePropertiesV2[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<FeaturePropertiesV2 | null>(null);
  const { userLocation, limitLayers, config, minimumSearchLength, children } =
    props;

  const debouncedSearchQuery = useDebounce(searchQuery, 500);

  const api = useMemo(() => new GeocodingApi(config), [config]);

  useEffect(() => {
    let ignore = false;

    async function startFetching() {
      setIsLoading(true);

      const response = await search(
        api,
        debouncedSearchQuery,
        userLocation,
        minimumSearchLength
      ).catch(async (e: unknown) => {
        return [] as FeaturePropertiesV2[];
      });

      if (!ignore) {
        setFeatures(response);
        setIsLoading(false);
      }
    }
    startFetching();

    return () => {
      ignore = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [api, debouncedSearchQuery, minimumSearchLength]);

  const handleResultSelected = (feature: FeaturePropertiesV2 | null) => {
    setResult(feature);
    props.onResultSelected?.(feature);
  };

  return (
    <AutocompleteSearchContext.Provider
      value={{
        api,
        searchQuery,
        setSearchQuery,
        result,
        setResult: handleResultSelected,
        results: features ?? [],
        isLoading,
        userLocation,
        limitLayers,
        config,
      }}
    >
      <View
        style={[
          {
            flexDirection: 'column',
            width: '100%',
            gap: 4,
          },
          props.style,
        ]}
      >
        {children}
      </View>
    </AutocompleteSearchContext.Provider>
  );
};

type AutocompleteSearchInputProps = {
  style?: StyleProp<TextStyle>;
};

export const AutocompleteSearchInput = ({
  style,
}: AutocompleteSearchInputProps) => {
  const { searchQuery, setSearchQuery, result, setResult, isLoading } =
    useContext(AutocompleteSearchContext);

  const handleSearch = (text: string) => {
    setSearchQuery(text);
    if (result != null) {
      setResult(null);
    }
  };

  return (
    <View style={searchBarStyle.container}>
      <TextInput
        placeholder="Search"
        value={searchQuery}
        style={[searchBarStyle.search, style]}
        onChangeText={(text) => handleSearch(text)}
      />
      {isLoading && (
        <ActivityIndicator size={'small'} style={{ marginLeft: 'auto' }} />
      )}
    </View>
  );
};

const searchBarStyle = StyleSheet.create({
  container: {
    flexDirection: 'row',
    borderColor: 'black',
    borderWidth: 1,
    borderRadius: 5,
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  search: {
    flexGrow: 1,
  },
});

type AutocompleteSearchResultsProps = {
  style?: StyleProp<ViewStyle>;
  itemStyle?: StyleProp<ViewStyle>;
};

export const AutocompleteSearchResults = ({
  style,
  itemStyle,
}: AutocompleteSearchResultsProps) => {
  const { setSearchQuery, result, setResult, results, api } = useContext(
    AutocompleteSearchContext
  );

  const handleResultSelection = async (newResult: FeaturePropertiesV2) => {
    const detail = await api.placeDetailsV2({
      ids: [newResult.properties.gid],
    });
    if (detail.features.length === 0) {
      console.error('Unexpected place lookup response with zero results');
      return;
    }
    setResult(detail.features[0]);
    if (detail.features[0].properties?.name) {
      setSearchQuery(detail.features[0].properties.name);
    }
    Keyboard.dismiss();
  };

  if (result != null || results?.length === 0) {
    return null;
  }

  return (
    <View style={[searchResultStyle.container, style]}>
      {results.map((feature) => (
        <Pressable
          onPress={() => handleResultSelection(feature)}
          key={feature.properties.gid}
          style={[searchResultStyle.item, itemStyle]}
        >
          {icon(feature.properties.layer)}
          <Text numberOfLines={1} style={{ flexShrink: 1 }}>
            {feature.properties?.name ?? '<No info>'}
          </Text>
          <Text style={{ marginLeft: 'auto' }}>
            {feature.properties.distance}
          </Text>
        </Pressable>
      ))}
    </View>
  );
};

const searchResultStyle = StyleSheet.create({
  container: {
    flexDirection: 'column',
    borderColor: 'black',
    borderWidth: 1,
    borderRadius: 5,
  },
  item: {
    flexDirection: 'row',
    marginHorizontal: 8,
    marginVertical: 4,
    gap: 4,
    alignItems: 'center',
  },
});

export const AutocompleteSearchRoot = (props: AutocompleteSearchRootProps) => {
  return <AutocompleteSearchRootBase {...props} />;
};
