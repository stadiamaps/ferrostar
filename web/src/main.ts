import { FerrostarMap } from "./ferrostar-map";
import { FerrostarCore } from "./ferrostar-core";
import { BrowserLocationProvider, SimulatedLocationProvider } from "./location";
export {
  FerrostarMap,
  BrowserLocationProvider,
  SimulatedLocationProvider,
  FerrostarCore,
};

declare global {
  interface HTMLElementTagNameMap {
    "ferrostar-map": FerrostarMap;
    "ferrostar-core": FerrostarCore;
  }
}
