import { defineConfig } from "vite";

export default defineConfig({
  base: "",
  server: {
    fs: {
      // Allow serving the wasm module
      allow: [".", "../common/ferrostar/pkg"],
    },
  },
  build: {
    lib: {
      entry: "src/main.ts",
      formats: ["es"],
    },
    rollupOptions: {
      external: ["ferrostar", "maplibre-gl", "lit", "@stadiamaps/maplibre-search-box"],
    },
  },
});
