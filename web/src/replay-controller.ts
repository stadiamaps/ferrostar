import {
  NavigationRecordingEvent,
  NavigationReplay,
  Route,
  TripState,
} from "@stadiamaps/ferrostar";
import { StateProvider } from "./types";
import { ReactiveElement } from "lit";
import { property, customElement, state } from "lit/decorators.js";

@customElement("replay-controller")
export class ReplayController extends ReactiveElement implements StateProvider {
  private replay: NavigationReplay;
  private current_event: NavigationRecordingEvent;
  private current_event_index: number;
  private current_timestamp: number;
  private route: Route;
  private allEvents: NavigationRecordingEvent[];
  private totalDuration: number;
  
  @state()
  private isPlaying: boolean = false;

  @state()
  private isPaused: boolean = false;

  @state()
  private playbackSpeed: number = 1;

  @state()
  private currentProgress: number = 0;

  @property({ type: Function, attribute: false })
  onNavigationStart?: () => void;

  @property({ type: Function, attribute: false })
  onNavigationStop?: () => void;

  constructor(json_str: string) {
    super();

    this.replay = new NavigationReplay(json_str);
    this.route = this.replay.getInitialRoute();
    this.current_timestamp = 0;
    this.current_event_index = 0;
    this.current_event = null as any;
    this.allEvents = this.replay.getAllEvents();
    this.totalDuration = this.replay.getTotalDuration();
  }

  async play() {
    if (this.isPlaying) return;
    
    this.isPlaying = true;
    this.isPaused = false;

    if (this.current_event_index === 0 && this.onNavigationStart) {
      this.onNavigationStart();
      this.provideRoute(this.route);
    }
  }

  pause() {
    if (this.isPaused) return;

    this.isPaused = true;
    this.isPlaying = false;
  
    // TODO: Write something like if last event, run onNavigationStop
  }

  setPlaybackSpeed(speed: number) {
    this.playbackSpeed = speed;
  }

  private delay(ms: number) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  async startReplay() {
    if (!this.replay) return;
    if (!this.route) return;
    if (this.onNavigationStart) this.onNavigationStart();
    this.provideRoute(this.route);

    while (true) {
      this.current_event = this.replay.getEventByIndex(this.current_event_index);
      this.current_event_index++;
      if (!this.current_event) return;


      if ("StateUpdate" in this.current_event.event_data) {
        this.provideState(this.current_event.event_data["StateUpdate"].trip_state);
      }

      await this.delay(this.current_event.timestamp - this.current_timestamp);

      this.current_timestamp = this.current_event.timestamp;
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
    // Dispatch Route for external listeners
    this.dispatchEvent(
      new CustomEvent("route-update", {
        detail: { route },
        bubbles: true,
      }),
    );
  }

  async stopNavigation() {
    this.replay = null as any;
    this.current_event = null as any;
    this.current_event_index = 0;
    this.current_timestamp = 0;
    this.route = null as any;
    if (this.onNavigationStop) this.onNavigationStop();
  }
}
