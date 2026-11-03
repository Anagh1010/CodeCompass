import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const backendStatic = fileURLToPath(new URL("../src/main/resources/static", import.meta.url));

export default defineConfig({
  plugins: [react()],
  server: {
    host: "127.0.0.1",
    port: 5173,
    proxy: {
      "/api": "http://localhost:8080",
      "/actuator": "http://localhost:8080",
    },
  },
  build: {
    outDir: backendStatic,
    emptyOutDir: true,
  },
});
