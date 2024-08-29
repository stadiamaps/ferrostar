import { FerrostarCore } from "./ferrostar-core";
import { BrowserLocationProvider, SimulatedLocationProvider } from "./location";
export { FerrostarCore, BrowserLocationProvider, SimulatedLocationProvider };

declare global {
    interface HTMLElementTagNameMap {
        "ferrostar-core": FerrostarCore;
    }
}
