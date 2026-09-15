import type { NavigationState } from './FerrostarCore';
import type { MaybePromise } from './LocationProvider';

export type ForegroundServiceStartOptions = {
  /** Stop the active Ferrostar navigation session. */
  stopNavigation(): void;
};

/**
 * Integrates navigation with a platform-specific foreground experience.
 *
 * Implementations can use an Android foreground service, an iOS Live Activity,
 * or any application-owned equivalent. Ferrostar calls the methods in order and
 * waits for each returned promise before beginning the next operation.
 */
export interface ForegroundService {
  start(options: ForegroundServiceStartOptions): MaybePromise<void>;
  update?(state: NavigationState): MaybePromise<void>;
  stop(): MaybePromise<void>;
}
