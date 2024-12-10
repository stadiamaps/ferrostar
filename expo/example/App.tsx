import { Ferrostar, FerrostarProvider } from "expo-ferrostar";
import { View } from "react-native";

export default function App() {
  return (
            <Ferrostar
                style={{ flex: 1 }}
                locationMode="default"
                styleUrl="https://api.maptiler.com/maps/basic-v2/style.json?key=DQTRKy0N0WUe90KUjN4i"
                snapUserLocationToRoute={false}
                navigationControllerConfig={{
                    stepAdvance: {
                        minimumHorizontalAccuracy: 25,
                        automaticAdvanceDistance: 10,
                    },
                    routeDeviationTracking: {
                        minimumHorizontalAccuracy: 15,
                        maxAcceptableDeviation: 50.0
                    },
                    courseFiltering: "SNAP_TO_ROUTE"
                }}
            />
  );
}

const styles = {
  container: {
    flex: 1,
    backgroundColor: "#FFF",
  },
};
