import type {
  NavigationControllerConfig,
  NavigationRecordingEvent,
} from '@stadiamaps/ferrostar-uniffi-react-native';

/**
 * A read-only handle to a navigation session recording.
 *
 * Ferrostar keeps recording in memory. The application decides if and where
 * the resulting bytes or JSON should be stored, uploaded, or shared.
 * Prefer MessagePack for long sessions to avoid allocating a large JavaScript string.
 */
export interface NavigationRecording {
  getEvents(): ReadonlyArray<NavigationRecordingEvent>;
  /** Exports the entire recording as JSON; large recordings can exceed Hermes string limits. */
  getRecordingJson(): string;
  /**
   * Exports a MessagePack snapshot with the same field names and values as the JSON schema.
   * This still allocates a complete binary buffer. Export after stopping navigation,
   * and avoid decoding the entire recording on memory-constrained devices.
   */
  getRecordingMsgpack(): ArrayBuffer;
}

export type RecordedNavigationOptions = {
  recording: true;
  config?: NavigationControllerConfig;
};
