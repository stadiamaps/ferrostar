import { useContext } from 'react';
import { FerrostarContext } from '../contexts/FerrostarProvider';

export function useFerrostar() {
  const context = useContext(FerrostarContext);
  if (!context) {
    throw new Error('useFerrostar must be used within a FerrostarProvider');
  }

  return context.core;
}
