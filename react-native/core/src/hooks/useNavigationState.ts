import { useEffect, useState } from 'react';
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
  const [uiState, setUiState] = useState<NavigationUiState>(() =>
    NavigationUiState.fromFerrostar(core._state, isMuted, core._lastLocation)
  );

  useEffect(() => {
    const listenerId = core.addStateListener((state) => {
      // Force a new NavigationUiState object so React knows to re-render
      setUiState(
        NavigationUiState.fromFerrostar(state, isMuted, core._lastLocation)
      );
    });

    return () => {
      core.removeStateListener(listenerId);
    };
  }, [core, isMuted]);

  return uiState;
}
