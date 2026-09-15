import {
  useNavigationState,
  useFerrostar,
} from '@stadiamaps/ferrostar-core-react-native';

export type NavigatingProps = {
  children: React.ReactNode;
};

export const Navigating = ({ children }: NavigatingProps) => {
  const core = useFerrostar();
  const uiState = useNavigationState(core);

  if (!uiState.isNavigating()) return null;

  return children;
};
