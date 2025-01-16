import { View, StyleSheet, Pressable, Text } from 'react-native';
import { getIcon } from './maneuver/_icons';

type MapControlsProps = {
  isNavigating?: boolean;
  isMuted?: boolean;
  onRoutePress?: () => void;
  onMutePress?: () => void;
  onZoomIn?: () => void;
  onZoomOut?: () => void;
};

const MapControls = ({
  isNavigating = false,
  isMuted = false,
  onMutePress = () => {},
  onRoutePress = () => {},
  onZoomIn = () => {},
  onZoomOut = () => {},
}: MapControlsProps) => {
  if (!isNavigating) return null;

  return (
    <>
      <View style={defaultStyle.topRightContainer}>
        <Pressable style={defaultStyle.routeButton} onPress={onRoutePress}>
          <Text>{getIcon('route', 32, 32)}</Text>
        </Pressable>
        <Pressable style={defaultStyle.muteButton} onPress={onMutePress}>
          <Text>
            {isMuted
              ? getIcon('volume_off', 32, 32)
              : getIcon('volume_up', 32, 32)}
          </Text>
        </Pressable>
      </View>
      <View style={defaultStyle.centerRightContainer}>
        <View style={defaultStyle.zoomContainer}>
          <Pressable style={defaultStyle.zoomInButton} onPress={onZoomIn}>
            <Text style={defaultStyle.text}>{getIcon('add', 32, 32)}</Text>
          </Pressable>
          <View style={defaultStyle.zoomDivider} />
          <Pressable style={defaultStyle.zoomOutButton} onPress={onZoomOut}>
            <Text style={defaultStyle.text}>{getIcon('remove', 32, 32)}</Text>
          </Pressable>
        </View>
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
  muteButton: {
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

export default MapControls;
