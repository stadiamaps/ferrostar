import {
  useFerrostar,
  useNavigationState,
} from '@stadiamaps/ferrostar-core-react-native';

export type NotNavigatingProps = {
  children: React.ReactNode;
};
export const NotNavigating = ({ children }: NotNavigatingProps) => {
  const core = useFerrostar();
  const uiState = useNavigationState(core);

  if (uiState.isNavigating()) return null;

  return children;
};
