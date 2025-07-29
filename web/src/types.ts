import { TripState } from "@stadiamaps/ferrostar";

export interface StateProvider {
    /**
     * Emits a navigation event.
     * @param tripState The current trip state.
     * 
     * Example declaration:
     * ```typescript
     * provideState(tripState: TripState) {
     *  this.dispatchEvent(
     *    new CustomEvent("tripstate-update", {
     *      detail: { tripState },
     *      bubbles: true,
     *    }),
     *  );
     * }
     * ```
     */
    provideState(tripState: TripState): void;

    /**
     * Optional callback for when navigation stops.
     */
    stopNavigation?(): void;
}
