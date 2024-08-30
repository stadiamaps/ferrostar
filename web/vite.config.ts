import { defineConfig } from "vite";
import dts from "vite-plugin-dts";
import wasmPack from "vite-plugin-wasm-pack";

export default defineConfig({
  base: "",
  build: {
    lib: {
      entry: "src/main.ts",
      name: "@stadiamaps/ferrostar-webcomponents",
    },
    rollupOptions: {
      external: ["@stadiamaps/ferrostar", "maplibre-gl", "lit", "@stadiamaps/maplibre-search-box"],
      output: {
        globals: {
          "lit": "lit",
          "maplibre-gl": "maplibregl",
          "@stadiamaps/maplibre-search-box": "maplibreSearchBox",
          "@stadiamaps/ferrostar": "ferrostar"
        },
      },
    },
    sourcemap: true,
  },
  plugins: [
      dts(),
      wasmPack(["../common/ferrostar"])
  ]
});
