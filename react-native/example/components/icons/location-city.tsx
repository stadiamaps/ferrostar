import Svg, { Path, type SvgProps } from 'react-native-svg';

function LocationCity(props: SvgProps) {
  return (
    <Svg
      height="24px"
      viewBox="0 -960 960 960"
      width="24px"
      fill="0"
      {...props}
    >
      <Path d="M120-120v-560h240v-80l120-120 120 120v240h240v400H120zm80-80h80v-80h-80v80zm0-160h80v-80h-80v80zm0-160h80v-80h-80v80zm240 320h80v-80h-80v80zm0-160h80v-80h-80v80zm0-160h80v-80h-80v80zm0-160h80v-80h-80v80zm240 480h80v-80h-80v80zm0-160h80v-80h-80v80z" />
    </Svg>
  );
}

export default LocationCity;
