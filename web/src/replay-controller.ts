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
  private route: Route;

  // Has the replay been started at least once
  @state()
  private hasStarted: boolean = false;

  // Is the replay currently playing
  @state()
  private isPlaying: boolean = false;

  // Playback speed multiplier (1 = normal speed)
  @state()
  private playbackSpeed: number = 1;

  // Percentage of replay completed (0-100)
  @state()
  private currentProgress: number = 0;

  // Index of the current event being processed
  @state()
  private current_event_index: number = 0;

  @state()
  private prev_timestamp: number = 0;

  @state()
  private total_duration: number = 0;

  @state()
  private allEvents: NavigationRecordingEvent[] = [];

  @property({ type: Function, attribute: false })
  onNavigationStart?: () => void;

  @property({ type: Function, attribute: false })
  onNavigationStop?: () => void;

  constructor(json_str: string) {
    super();

    this.replay = new NavigationReplay(json_str);
    this.route = this.replay.getInitialRoute();
    this.allEvents = this.replay.getAllEvents();
    this.current_event = null as any;
    this.total_duration = this.replay.getTotalDuration();
  }

  // Apply delay based on timestamps and playback speed
  private applyDelay() {
    const delay = (this.current_event.timestamp - this.prev_timestamp) / this.playbackSpeed;

    return new Promise((resolve) => setTimeout(resolve, delay));
  }

  async startReplay() {
    if (!this.replay) return;
    if (!this.route) return;

    // Only run on first play
    if (!this.hasStarted) {
      this.hasStarted = true;
      if (this.onNavigationStart) this.onNavigationStart();
      this.provideRoute(this.route);
    }

    while (this.isPlaying) {
      // Check if we reached the end of the replay
      if (this.current_event_index >= this.allEvents.length) {
        this.stopNavigation();
        return;
      }

      // Get the next event
      this.current_event = this.replay.getEventByIndex(this.current_event_index);
      if (!this.current_event) {
        this.stopNavigation();
        return;
      }

      // Apply the state if it's a state update
      if ("StateUpdate" in this.current_event.event_data) {
        this.provideState(this.current_event.event_data["StateUpdate"].trip_state);
      }

      // Update progress and delay for the next event
      this.currentProgress = (this.current_event_index / this.allEvents.length) * 100;
      await this.applyDelay();

      // If paused during the delay, break the loop
      if (!this.isPlaying) {
        break;
      }

      this.prev_timestamp = this.current_event.timestamp;
      this.current_event_index++;
    }
  }

  async play() {
    if (this.isPlaying) return;

    this.isPlaying = true;
    await this.startReplay();
  }

  pause() {
    if (!this.isPlaying) return;

    this.isPlaying = false;
  }

  setPlaybackSpeed(speed: number) {
    if (speed <= 0) return;
    this.playbackSpeed = speed;
  }

  seekToIndex(index: number) {
    if (index < 0 || index >= this.allEvents.length) return;

    this.current_event_index = index;
    this.current_event = this.replay.getEventByIndex(this.current_event_index);
    this.currentProgress = (this.current_event_index / this.allEvents.length) * 100;

    // Provide the state of the current event if it's a state update
    if (this.current_event && "StateUpdate" in this.current_event.event_data) {
      this.provideState(this.current_event.event_data["StateUpdate"].trip_state);
    }

    this.prev_timestamp = this.current_event.timestamp;
  }

  seekToProgress(progress: number) {
    if (progress < 0 || progress > 100) return;

    // Calculate the index based on progress
    const index = Math.floor((progress / 100) * this.allEvents.length);
    this.seekToIndex(index);
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
    this.prev_timestamp = 0;
    this.route = null as any;
    this.hasStarted = false;
    if (this.onNavigationStop) this.onNavigationStop();
  }

  // Getters for external access
  get progress() {return this.currentProgress}
  get speed() {return this.playbackSpeed}
  get playing() {return this.isPlaying}
  get duration() {return this.total_duration}
}
