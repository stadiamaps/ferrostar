import { useCallback, useMemo, useSyncExternalStore } from 'react';
import type { FerrostarCore } from '../FerrostarCore';
import { NavigationUiState } from '../NavigationUiState';

/**
 * A hook that subscribes to a FerrostarCore instance and returns
 * the current `NavigationUiState`.
 *
 * This hook manages the `addStateListener` / `removeStateListener` lifecycle
 * automatically and triggers React re-renders when the state changes.
 */
export function useNavigationState(
  core: FerrostarCore,
  isMuted?: boolean
): NavigationUiState {
  const subscribe = useCallback(
    (onStoreChange: () => void) => {
      const listenerId = core.addStateListener(onStoreChange);
      return () => core.removeStateListener(listenerId);
    },
    [core]
  );
  const getSnapshot = useCallback(() => core._stateRevision, [core]);
  const stateRevision = useSyncExternalStore(
    subscribe,
    getSnapshot,
    getSnapshot
  );

  return useMemo(
    () =>
      NavigationUiState.fromFerrostar(
        core._state,
        isMuted ?? core._isMuted,
        core._lastLocation,
        core._lastHeading
      ),
    [core, isMuted, stateRevision]
  );
}
