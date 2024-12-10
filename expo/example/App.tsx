import { Ferrostar, FerrostarProvider } from "expo-ferrostar";
import { SafeAreaView } from "react-native";

export default function App() {
  return (
    <SafeAreaView style={styles.container}>
      <FerrostarProvider>
        <Ferrostar style={{ flex: 1, width: "100%" }} />
      </FerrostarProvider>
    </SafeAreaView>
  );
}

const styles = {
  container: {
    flex: 1,
    backgroundColor: "#eee",
  },
};
