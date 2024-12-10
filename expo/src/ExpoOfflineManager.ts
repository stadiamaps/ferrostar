import { requireNativeModule } from "expo";

import { ExpoOfflineManagerModule } from "./ExpoOfflineManager.types";

const ExpoOfflineManagerNativeModule =
  requireNativeModule<ExpoOfflineManagerModule>("ExpoOfflineManager");

export default ExpoOfflineManagerNativeModule;
