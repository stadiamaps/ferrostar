// https://docs.expo.dev/guides/using-eslint/
const { defineConfig } = require('eslint/config');
const expoConfig = require('eslint-config-expo/flat');
const reactYouMightNotNeedAnEffect = require('eslint-plugin-react-you-might-not-need-an-effect');

module.exports = defineConfig([
  expoConfig,
  reactYouMightNotNeedAnEffect.configs.recommended,
  {
    ignores: ['dist/*'],
  },
]);
