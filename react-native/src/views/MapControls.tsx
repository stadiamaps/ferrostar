import { View, StyleSheet, Pressable, Text } from 'react-native';

const MapControls = () => {
  return (
    <>
      <View style={defaultStyle.topRightContainer}>
        <Pressable style={defaultStyle.muteButton}>
          <Text>Mute</Text>
        </Pressable>
        <View style={defaultStyle.zoomContainer}>
          <Pressable style={defaultStyle.zoomInButton}>
            <Text style={defaultStyle.text}>+</Text>
          </Pressable>
          <View style={defaultStyle.zoomDivider} />
          <Pressable style={defaultStyle.zoomOutButton}>
            <Text style={defaultStyle.text}>-</Text>
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
  muteButton: {
    borderRadius: 100,
    width: 52,
    height: 52,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
    margin: 10,
  },
  zoomContainer: {
    flexDirection: 'column',
  },
  zoomDivider: {
    height: 1,
    width: '100%',
    backgroundColor: '#000',
  },
  zoomInButton: {
    borderTopLeftRadius: 100,
    borderTopRightRadius: 100,
    width: 52,
    height: 52,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
  },
  zoomOutButton: {
    borderBottomLeftRadius: 100,
    borderBottomRightRadius: 100,
    width: 52,
    height: 52,
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
