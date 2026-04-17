import Svg, { Path, type SvgProps } from 'react-native-svg';

function PostBox(props: SvgProps) {
  return (
    <Svg
      height="24px"
      viewBox="0 -960 960 960"
      width="24px"
      fill="0"
      {...props}
    >
      <Path d="M640-200v80q0 17-11.5 28.5T600-80H120q-17 0-28.5-11.5T80-120v-320q0-17 11.5-28.5T120-480h120v-160q0-100 70-170t170-70h160q100 0 170 70t70 170v560h-80v-120H640zm0-80h160v-360q0-66-47-113t-113-47H480q-66 0-113 47t-47 113v160h280q17 0 28.5 11.5T640-440v160zM400-560v-80h320v80H400zm-40 274l200-114H160l200 114zm0 70L160-330v170h400v-170L360-216zM160-400v240-240z" />
    </Svg>
  );
}

export default PostBox;
