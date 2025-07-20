import {
  NavigationRecordingEvent,
  NavigationReplay,
} from "@stadiamaps/ferrostar";

export class Replay {
  replayController: NavigationReplay;
  current_event_index: number;
  current_event: NavigationRecordingEvent | undefined;
  initial_timestamp: number;

  constructor(json_str: string) {
    this.replayController = new NavigationReplay(json_str);
    this.current_event_index = 0;
    this.current_event = undefined;
    this.initial_timestamp = this.replayController.getInitialTimestamp();
  }

  startReplay() {
    while (true) {
      this.getEvent();
      if (this.current_event == undefined) {
        return;
      }
      // console.log(this.replayController.);

      if (this.isNavStateUpdate()) {
        this.getNavState();
      }
    }
  }

  getEvent() {
    this.current_event = this.replayController.getNextEvent(
      this.current_event_index,
    );
    this.current_event_index++;
  }

  isNavStateUpdate(): boolean {
    let eventData = this.current_event?.event_data;
    // @ts-ignore
    return eventData["NavStateUpdate"] != undefined;
  }

  getNavState() {
    if (this.current_event == undefined) {
      return undefined;
    }
    let eventData = this.current_event.event_data;
    // @ts-ignore
    return eventData["NavStateUpdate"];
  }
}
