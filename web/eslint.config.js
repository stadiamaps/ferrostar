import tseslint from "@typescript-eslint/eslint-plugin";
import tsParser from "@typescript-eslint/parser";
import litPlugin from "eslint-plugin-lit";
import wcPlugin from "eslint-plugin-wc";
import js from "@eslint/js";
import prettier from "eslint-config-prettier";
import globals from "globals";

export default [
  js.configs.recommended,
  {
    files: ["**/*.{js,ts,html}"],
    ignores: ["dist/**", "node_modules/**"],
    languageOptions: {
      parser: tsParser,
      parserOptions: {
        ecmaVersion: 2022,
        sourceType: "module",
        project: "./tsconfig.json",
      },
      ecmaVersion: 2022,
      globals: {
        ...globals.browser,
        ...globals.worker,
      },
    },
    plugins: {
      "@typescript-eslint": tseslint,
      "lit": litPlugin,
      "wc": wcPlugin,
    },
    rules: {
      ...tseslint.configs.recommended.rules,
      ...litPlugin.configs.recommended.rules,
      ...wcPlugin.configs.recommended.rules,
      "wc/no-constructor-params": "off",
      "lit/no-template-arrow": "off",
      "@typescript-eslint/no-explicit-any": "off",
      "@typescript-eslint/no-unused-expressions": "warn",
      "@typescript-eslint/no-unsafe-function-type": "warn",
      "no-undef": "off",
      "@typescript-eslint/no-unused-vars": "warn",
      "@typescript-eslint/ban-ts-comment": "warn",
      "@typescript-eslint/no-unused-vars": [
        "error",
        {
          args: "none", // Function args can be useful documentation
          ignoreRestSiblings: true, // Allow `omit` syntax
        },
      ],
    },
  },
  prettier,
];
