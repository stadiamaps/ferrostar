import { useEffect, useState } from 'react';
import {
  watchPositionAsync,
  LocationSubscription,
  LocationObject,
} from 'expo-location';

export const useLocationTracker = () => {
  const [subscription, setSubscription] = useState<LocationSubscription | null>(
    null
  );
  const [currentPosition, setCurrentPosition] = useState<LocationObject | null>(
    null
  );
  const [locationError, setLocationError] = useState<string | null>(null);

  useEffect(() => {
    const startWatchingPosition = async () => {
      try {
        const positionSubscription = await watchPositionAsync(
          {
            accuracy: 6,
            timeInterval: 1000,
          },
          (l) => {
            setCurrentPosition(l);
            setLocationError(null);
          },
          (error) => {
            setLocationError('Location tracking error');
            setCurrentPosition(null);
          }
        );

        setSubscription(positionSubscription);
      } catch (_) {
        setLocationError('Location tracking error');
        setCurrentPosition(null);
      }
    };

    startWatchingPosition();

    return () => {
      if (subscription) {
        subscription.remove();
      }
    };
  }, [subscription]);

  return { currentPosition, locationError };
};
