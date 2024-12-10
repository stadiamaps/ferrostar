import { createContext, ReactNode, useState } from "react";

import { ExpoFerrostarModule } from "./ExpoFerrostar.types";

type FerrostarContext = {
  refs: {
    [id: string]: ExpoFerrostarModule | undefined;
  };
};

export const FerrostarContext = createContext<FerrostarContext>({
  refs: {
    current: undefined,
  },
});

type FerrostarProviderProps = {
  children: ReactNode;
};

export const FerrostarProvider = ({ children }: FerrostarProviderProps) => {
  const [refs] = useState({ current: undefined });

  return (
    <FerrostarContext.Provider value={{ refs }}>
      {children}
    </FerrostarContext.Provider>
  );
};
