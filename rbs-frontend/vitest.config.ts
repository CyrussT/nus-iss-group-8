import { defineVitestConfig } from "@nuxt/test-utils/config";
import { fileURLToPath } from "node:url";

export default defineVitestConfig({
  plugins: [],
  test: {
    globals: true,
    environment: "happy-dom",
    setupFiles: ["./test/setup.ts"],
    include: ["**/__tests__/**/*.spec.ts"],
    deps: {
      inline: ["@nuxt/ui", "@nuxthq/ui"],
    },
    coverage: {
      // provider: "v8",
      reporter: ["text", "json", "html"],
      exclude: [
        "node_modules/**",
        ".nuxt/**",
        "dist/**",
        ".output/**",
        "test/**",
        "**/*.d.ts",
        "**/__tests__/**",
        "**/mocks/**",
        "coverage/**",
        "virtual:**", 
        "**/virtual:**", 
        "**virtual**",
        ".nuxt/color-mode-options.mjs",
        "**/.nuxt/color-mode-options.mjs",
        ".nuxt/*.mjs",
        "middleware/**",
        "server/**",
        "**/*.config.ts",
        "app.vue"
      ],
    },
  },
  resolve: {
    alias: {
      "~": fileURLToPath(new URL(".", import.meta.url)),
      "@": fileURLToPath(new URL(".", import.meta.url)),
    },
  },
});
