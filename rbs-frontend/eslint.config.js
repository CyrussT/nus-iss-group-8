import vuePlugin from "eslint-plugin-vue";
import tsPlugin from "@typescript-eslint/eslint-plugin";
import tsParser from "@typescript-eslint/parser";
import vueParser from "vue-eslint-parser"; // Add this
import eslintConfigPrettier from "eslint-config-prettier";

export default [
  // Apply Prettier configuration
  eslintConfigPrettier,
  {
    // Apply this configuration to specific file types
    files: ["**/*.js", "**/*.jsx", "**/*.ts", "**/*.tsx", "**/*.vue"],
    // Environment settings
    languageOptions: {
      ecmaVersion: "latest",
      sourceType: "module",
      globals: {
        browser: true,
        node: true,
      },
      // Use vue-eslint-parser for Vue files
      parser: vueParser,
      parserOptions: {
        parser: tsParser,
        extraFileExtensions: [".vue"], // Allow parsing .vue files
      },
    },
    // Plugins
    plugins: {
      vue: vuePlugin,
      "@typescript-eslint": tsPlugin,
    },
    // Rules
    rules: {
      // JavaScript Rules
      "no-unused-vars": "warn",
      "no-console": "warn",
      "eqeqeq": "error",
      "no-implicit-globals": "error",
      "no-var": "error",
      "prefer-const": "warn",

      // TypeScript Rules
      "@typescript-eslint/no-unused-vars": "warn",
      "@typescript-eslint/no-explicit-any": "warn",
      "@typescript-eslint/no-non-null-assertion": "warn",
      "@typescript-eslint/consistent-type-definitions": ["error", "interface"],

    },
  },
];