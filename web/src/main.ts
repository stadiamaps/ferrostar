import { FerrostarMap } from "./ferrostar-map";
import { BrowserLocationProvider, SimulatedLocationProvider } from "./location";
export { FerrostarMap, BrowserLocationProvider, SimulatedLocationProvider };

declare global {
  interface HTMLElementTagNameMap {
    "ferrostar-map": FerrostarMap;
  }
}
