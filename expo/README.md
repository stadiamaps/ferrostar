# Expo Ferrostar

This library is still in development and is not ready for production use.

## Status

- [x] Ferrostar Core Android Implementation
- [x] Ferrostar View Android Implementation
- [ ] Ferrostar Core iOS Implementation
- [ ] Ferrostar View iOS Implementation
- [ ] Documentation

This is a library for using [Ferrostar](https://github.com/stadiamaps/ferrostar) navigation library for [Expo](https://expo.dev/).

## Installation

```sh
expo install expo-ferrostar
```

## Simple Usage

```tsx
import { Ferrostar } from "expo-ferrostar";

export default function App() {
  return (
    <SafeAreaView style={styles.container}>
      <Ferrostar style={{ flex: 1, width: "100%" }} />
    </SafeAreaView>
  );
}
```
