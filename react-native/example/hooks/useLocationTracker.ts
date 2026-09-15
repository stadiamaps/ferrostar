import { useEffect, useState } from 'react';
import {
  watchPositionAsync,
  LocationSubscription,
  LocationObject,
} from 'expo-location';

export const useLocationTracker = () => {
  const [currentPosition, setCurrentPosition] = useState<LocationObject | null>(
    null
  );
  const [locationError, setLocationError] = useState<string | null>(null);

  useEffect(() => {
    let subscription: LocationSubscription | undefined;
    let cancelled = false;

    const startWatchingPosition = async () => {
      try {
        const positionSubscription = await watchPositionAsync(
          {
            accuracy: 6,
            timeInterval: 1000,
          },
          (l) => {
            if (cancelled) {
              return;
            }
            setCurrentPosition(l);
            setLocationError(null);
          },
          () => {
            if (cancelled) {
              return;
            }
            setLocationError('Location tracking error');
            setCurrentPosition(null);
          }
        );

        if (cancelled) {
          positionSubscription.remove();
        } else {
          subscription = positionSubscription;
        }
      } catch {
        if (cancelled) {
          return;
        }
        setLocationError('Location tracking error');
        setCurrentPosition(null);
      }
    };

    void startWatchingPosition();

    return () => {
      cancelled = true;
      subscription?.remove();
    };
  }, []);

  return { currentPosition, locationError };
};
