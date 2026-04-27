import { useContext } from 'react';
import { FerrostarCore } from '../FerrostarCore';
import { FerrostarContext } from '../contexts/FerrostarProvider';

export function useFerrostar(): FerrostarCore {
  const context = useContext(FerrostarContext);

  if (!context) {
    throw new Error('useFerrostar must be used within a FerrostarProvider');
  }

  return context.core;
}
