import Svg, { Path, type SvgProps } from 'react-native-svg';

function Road(props: SvgProps) {
  return (
    <Svg
      height="24px"
      viewBox="0 -960 960 960"
      width="24px"
      fill="0"
      {...props}
    >
      <Path d="M160-160v-640h80v640h-80zm280 0v-160h80v160h-80zm280 0v-640h80v640h-80zM440-400v-160h80v160h-80zm0-240v-160h80v160h-80z" />
    </Svg>
  );
}

export default Road;
