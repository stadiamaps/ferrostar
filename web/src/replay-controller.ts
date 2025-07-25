import { NavigationRecordingEvent, NavigationReplay } from "@stadiamaps/ferrostar";

export class ReplayController {
  private replay: NavigationReplay;
  private current_event_index: number = 0;
  public onNavStateUpdate: (tripState: any, stepAdvanceCondition: any) => void = () => {};

  constructor(json_str: string) {
    this.replay = new NavigationReplay(json_str);
  }

  start() {
    this.nextEvent();
  }

  stop() {}

  nextEvent() {
    const event: NavigationRecordingEvent | undefined =
      this.replay.getNextEvent(this.current_event_index);
    if (!event) return;

    let eventData = event.event_data;
    if ("StateUpdate" in eventData) {
      const stateUpdate = eventData["StateUpdate"];
      this.onNavStateUpdate(stateUpdate.trip_state, stateUpdate.step_advance_condition);
    }

    this.current_event_index++;
    this.nextEvent();
  }
}
