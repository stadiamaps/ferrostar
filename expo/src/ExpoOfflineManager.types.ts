import { BoundingBox } from "./ExpoFerrostar.types";

export type OfflineRegionDefinition = {
  bounds: BoundingBox;
  includeIdeographs: boolean;
  maxZoom: number;
  minZoom: number;
  pixelRatio: number;
  styleURL?: string;
  type: "tileregion" | "shaperegion";
};

export type OfflineRegion = {
  definition: OfflineRegionDefinition;
  id: number;
  metadata: string;
  isDeliveringInactiveMessages: boolean;
};

export type ExpoOfflineManagerModule = {
  createOfflineRegion: (
    definition: OfflineRegionDefinition,
    metadata: string
  ) => Promise<OfflineRegion>;
  resetDatabase: () => Promise<void>;
  packDatabase: () => Promise<void>;
  listOfflineRegions: () => Promise<OfflineRegion[]>;
  runPackDatabaseAutomatically: (autopack: boolean) => Promise<void>;
  setMaximumAmbientCacheSize: (size: number) => Promise<void>;
};
