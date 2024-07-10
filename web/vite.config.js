import { defineConfig } from "vite";

export default defineConfig({
  server: {
    fs: {
      // Allow serving the wasm module
      allow: [".", "../common/ferrostar/pkg"],
    },
  },
});
