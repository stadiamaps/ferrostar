import { useCallback, useMemo, useSyncExternalStore } from 'react';
import { TripState } from '@stadiamaps/ferrostar-uniffi-react-native';
import type { NavigationState } from '../FerrostarCore';
import {
  decodeAnnotation,
  type AnnotationParser,
  type AnnotationResult,
} from '../annotations/Annotation';
import { useFerrostar } from './useFerrostar';

/**
 * Decode and observe the annotation for the user's current route segment.
 *
 * The parser should have a stable identity, such as an imported function or a
 * function memoized by the caller. Annotation parsing is synchronous and only
 * reruns when the annotation JSON or parser changes.
 */
export function useAnnotation<T>(
  parser: AnnotationParser<T>
): AnnotationResult<T> {
  const core = useFerrostar();
  const subscribe = useCallback(
    (onStoreChange: () => void) => {
      const listenerId = core.addStateListener(onStoreChange);

      return () => {
        core.removeStateListener(listenerId);
      };
    },
    [core]
  );
  const getSnapshot = useCallback(
    () => currentAnnotationJson(core._state),
    [core]
  );
  const annotationJson = useSyncExternalStore(subscribe, getSnapshot);

  return useMemo(
    () => decodeAnnotation(annotationJson, parser),
    [annotationJson, parser]
  );
}

function currentAnnotationJson(state: NavigationState): string | undefined {
  const tripState = state.tripState;
  if (!tripState || !TripState.Navigating.instanceOf(tripState)) {
    return undefined;
  }

  return tripState.inner.annotationJson;
}
