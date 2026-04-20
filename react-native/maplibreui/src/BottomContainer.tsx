import { StyleSheet, View } from 'react-native';

type BottomContainerProps = {
  children: React.ReactNode;
};

export const BottomContainer = ({ children }: BottomContainerProps) => {
  return <View style={defaultStyles.container}>{children}</View>;
};

const defaultStyles = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'column',
    rowGap: 5,
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    margin: 10,
  },
});
