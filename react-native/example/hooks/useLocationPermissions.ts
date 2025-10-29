import { useEffect, useState } from 'react';
import * as Location from 'expo-location';

export const useLocationPermission = () => {
  const [isPermissionGranted, setIsPermissionGranted] = useState(false);
  const [permissionError, setPermissionError] = useState<string | null>(null);

  useEffect(() => {
    const requestPermission = async () => {
      try {
        const { status } = await Location.requestForegroundPermissionsAsync();

        if (status !== 'granted') {
          setPermissionError('Location permission not granted');
          setIsPermissionGranted(false);
          return;
        }

        setIsPermissionGranted(true);
        setPermissionError(null);
      } catch (_) {
        setPermissionError('Location permission error');
        setIsPermissionGranted(false);
      }
    };

    requestPermission();
  }, []);

  return { isPermissionGranted, permissionError };
};
