import { View, StyleSheet, Pressable, Text } from 'react-native';
import { getIcon } from './maneuver/_icons';
import {
  useFerrostar,
  useNavigationState,
} from '@stadiamaps/ferrostar-core-react-native';
import { useCamera } from './hooks/useCamera';

type MapControlsProps = {
  onMutePress?: () => void;
};

export const MapControls = ({ onMutePress = () => {} }: MapControlsProps) => {
  const core = useFerrostar();
  const uiState = useNavigationState(core);
  const { cameraMode, zoomIn, zoomOut, recenter, overview } = useCamera();

  if (!uiState.isNavigating()) return null;

  return (
    <>
      {cameraMode === 'following' && (
        <View style={defaultStyle.topRightContainer}>
          <Pressable style={[defaultStyle.routeButton]} onPress={overview}>
            <Text>{getIcon('route', 32, 32)}</Text>
          </Pressable>
          <Pressable style={defaultStyle.muteButton} onPress={onMutePress}>
            <Text>
              {uiState.isMuted
                ? getIcon('volume_off', 32, 32)
                : getIcon('volume_up', 32, 32)}
            </Text>
          </Pressable>
        </View>
      )}
      <View style={defaultStyle.centerRightContainer}>
        <View style={defaultStyle.zoomContainer}>
          <Pressable style={defaultStyle.zoomInButton} onPress={zoomIn}>
            <Text style={defaultStyle.text}>{getIcon('add', 32, 32)}</Text>
          </Pressable>
          <View style={defaultStyle.zoomDivider} />
          <Pressable style={defaultStyle.zoomOutButton} onPress={zoomOut}>
            <Text style={defaultStyle.text}>{getIcon('remove', 32, 32)}</Text>
          </Pressable>
        </View>
      </View>
      <View style={defaultStyle.bottomRightContainer}>
        {cameraMode !== 'following' && (
          <Pressable style={defaultStyle.recenterButton} onPress={recenter}>
            <Text>{getIcon('near_me', 32, 32, '#007AFF')}</Text>
          </Pressable>
        )}
      </View>
    </>
  );
};

const defaultStyle = StyleSheet.create({
  topRightContainer: {
    position: 'absolute',
    top: 90,
    right: 0,
    flexDirection: 'column',
    justifyContent: 'center',
    alignItems: 'center',
    marginVertical: 10,
  },
  centerRightContainer: {
    position: 'absolute',
    top: 0,
    bottom: 0,
    right: 0,
    flexDirection: 'column',
    justifyContent: 'center',
    alignItems: 'center',
    marginVertical: 10,
  },
  bottomRightContainer: {
    position: 'absolute',
    bottom: 90,
    right: 0,
    flexDirection: 'column',
    justifyContent: 'center',
    alignItems: 'center',
    marginVertical: 10,
  },
  muteButton: {
    borderRadius: 100,
    width: 60,
    height: 60,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
    margin: 10,
  },
  recenterButton: {
    borderRadius: 100,
    width: 60,
    height: 60,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
    margin: 10,
  },
  routeButton: {
    borderRadius: 100,
    width: 60,
    height: 60,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
    margin: 10,
  },
  activeButton: {
    backgroundColor: '#eee',
  },
  zoomContainer: {
    flexDirection: 'column',
    margin: 10,
  },
  zoomDivider: {
    height: 1,
    width: '100%',
    backgroundColor: '#000',
  },
  zoomInButton: {
    borderTopLeftRadius: 100,
    borderTopRightRadius: 100,
    width: 60,
    height: 60,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
  },
  zoomOutButton: {
    borderBottomLeftRadius: 100,
    borderBottomRightRadius: 100,
    width: 60,
    height: 60,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
  },
  text: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#000',
  },
});
