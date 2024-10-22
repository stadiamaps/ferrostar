import { defineConfig } from "vite";
import dts from "vite-plugin-dts";
import wasm from "vite-plugin-wasm";
import topLevelAwait from "vite-plugin-top-level-await";

export default defineConfig({
  base: "",
  build: {
    lib: {
      entry: "src/main.ts",
      name: "@stadiamaps/ferrostar-webcomponents",
    },
    rollupOptions: {
      external: [
        "@stadiamaps/ferrostar",
        "maplibre-gl",
        "lit",
        "@stadiamaps/maplibre-search-box",
      ],
      output: {
        globals: {
          "lit": "lit",
          "maplibre-gl": "maplibregl",
          "@stadiamaps/maplibre-search-box": "maplibreSearchBox",
          "@stadiamaps/ferrostar": "ferrostar",
        },
      },
    },
    sourcemap: true,
  },
  plugins: [dts(), topLevelAwait(), wasm()],
});
