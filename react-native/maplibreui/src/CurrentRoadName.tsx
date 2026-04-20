import {
  useFerrostar,
  useNavigationState,
} from '@stadiamaps/ferrostar-core-react-native';
import { StyleSheet, Text, View } from 'react-native';

export const CurrentRoadName = () => {
  const core = useFerrostar();
  const { currentStepRoadName } = useNavigationState(core);

  if (!currentStepRoadName) return null;

  return (
    <View style={styles.container}>
      <Text style={styles.text}>{currentStepRoadName}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'row',
    marginHorizontal: 'auto',
    backgroundColor: '#3583DD',
    borderColor: '#fff',
    borderWidth: 2.5,
    borderRadius: 100,
    paddingVertical: 5,
    paddingHorizontal: 10,
  },
  text: {
    color: '#fff',
    fontWeight: 'bold',
  },
});
