import type {
  NavigationControllerConfig,
  NavigationRecordingEvent,
} from '@stadiamaps/ferrostar-uniffi-react-native';

/**
 * A read-only handle to a navigation session recording.
 *
 * Ferrostar keeps recording in memory. The application decides if and where
 * the resulting JSON should be stored, uploaded, or shared.
 */
export interface NavigationRecording {
  getEvents(): ReadonlyArray<NavigationRecordingEvent>;
  getRecordingJson(): string;
}

export type RecordedNavigationOptions = {
  recording: true;
  config?: NavigationControllerConfig;
};
