import { useContext } from "react";

import { ExpoFerrostarModule } from "../ExpoFerrostar.types";
import { FerrostarContext } from "../FerrostarProvider";

const useNavigation = () => {
  const { refs } = useContext(FerrostarContext);

  return {
    ...refs,
    current: refs.current,
  } as {
    [id: string]: ExpoFerrostarModule | undefined;
    current?: ExpoFerrostarModule;
  };
};

export default useNavigation;
