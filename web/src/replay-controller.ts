import {
  NavigationRecordingEvent,
  NavigationReplay,
  Route,
  TripState,
} from "@stadiamaps/ferrostar";
import { StateProvider } from "./types";
import { ReactiveElement } from "lit";
import { property, customElement } from "lit/decorators.js";

@customElement("replay-controller")
export class ReplayController extends ReactiveElement implements StateProvider {
  private replay: NavigationReplay;
  private event: NavigationRecordingEvent;
  private current_event_index: number;
  private current_timestamp: number;
  private route: Route;

  @property({ type: Function, attribute: false })
  onNavigationStart?: () => void;

  @property({ type: Function, attribute: false })
  onNavigationStop?: () => void;

  constructor(json_str: string) {
    super();

    this.replay = new NavigationReplay(json_str);
    this.route = this.replay.getInitialRoute();
    this.event = this.replay.getNextEvent(0);
    this.current_timestamp = this.event.timestamp;
    this.current_event_index = 1;
  }

  private delay(ms: number) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  async startReplay() {
    if (!this.replay) return;
    if (!this.route) return;
    if (!this.event) return;
    if (this.onNavigationStart) this.onNavigationStart();
    this.provideRoute(this.route);

    while (true) {
      if ("StateUpdate" in this.event.event_data) {
        this.provideState(this.event.event_data["StateUpdate"].trip_state);
      }

      this.event = this.replay.getNextEvent(this.current_event_index);
      this.current_event_index++;
      if (!this.event) return;

      await this.delay(this.event.timestamp - this.current_timestamp);

      this.current_timestamp = this.event.timestamp;
    }
  }

  provideState(tripState: TripState) {
    // Dispatch event for external listeners
    this.dispatchEvent(
      new CustomEvent("tripstate-update", {
        detail: { tripState },
        bubbles: true,
      }),
    );
  }

  provideRoute(route: Route) {
    this.dispatchEvent(
      new CustomEvent("route-update", {
        detail: { route },
        bubbles: true,
      }),
    );
  }

  async stopNavigation() {
    this.replay = null as any;
    this.event = null as any;
    this.current_event_index = 0;
    this.current_timestamp = 0;
    this.route = null as any;
    if (this.onNavigationStop) this.onNavigationStop();
  }
}
