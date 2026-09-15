import { CameraProviderContext } from '../contexts/CameraProvider';
import { useContext } from 'react';

export const useCamera = () => {
  const context = useContext(CameraProviderContext);
  if (!context) {
    throw new Error('useCamera must be used within a CameraProvider');
  }
  return context;
};
