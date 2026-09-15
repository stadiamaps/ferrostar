import globals from 'globals';
import pluginJs from '@eslint/js';
import { defineConfig } from 'eslint/config';
import reactYouMightNotNeedAnEffect from 'eslint-plugin-react-you-might-not-need-an-effect';
import tseslint from 'typescript-eslint';

export default defineConfig([
  { ignores: ['**/lib/**', '**/*.config.{js,mjs,cjs,ts}'] },
  { files: ['**/*.{ts,tsx}'] },
  { languageOptions: { globals: globals.browser } },
  pluginJs.configs.recommended,
  ...tseslint.configs.recommended,
  reactYouMightNotNeedAnEffect.configs.recommended,
]);
