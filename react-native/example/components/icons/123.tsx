import Svg, { Path, type SvgProps } from 'react-native-svg';

function OneTwoThree(props: SvgProps) {
  return (
    <Svg height="24px" viewBox="0 -960 960 960" width="24px" {...props}>
      <Path d="M220-360v-180h-60v-60h120v240h-60zm140 0v-100q0-17 11.5-28.5T400-500h80v-40H360v-60h140q17 0 28.5 11.5T540-560v60q0 17-11.5 28.5T500-460h-80v40h120v60H360zm240 0v-60h120v-40h-80v-40h80v-40H600v-60h140q17 0 28.5 11.5T780-560v160q0 17-11.5 28.5T740-360H600z" />
    </Svg>
  );
}

export default OneTwoThree;
