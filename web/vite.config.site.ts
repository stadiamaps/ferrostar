import { resolve } from "path";
import { defineConfig } from "vite";
import topLevelAwait from "vite-plugin-top-level-await";
import wasm from "vite-plugin-wasm";

export default defineConfig({
  base: "",
  server: {
    fs: {
      // Allow serving the wasm module
      allow: [".", "../common/ferrostar/pkg"],
    },
  },
  build: {
    emptyOutDir: false,
    rollupOptions: {
      input: {
        main: resolve(__dirname, "index.html"),
        replay: resolve(__dirname, "tools/replay/index.html"),
      },
    },
  },
  plugins: [topLevelAwait(), wasm()],
});
