# General guidelines

This document captures code conventions for Ferrostar, a cross-platform turn-by-turn navigation SDK.
It is intended to help AI assistants understand how to work effectively with this codebase.

## For humans

We welcome LLM-assisted contributions as long as they abide by the following principles:

* **Aim for excellence.** LLMs are often viewed as a speed multiplier, but we value quality over speed. Invest the time savings in improving quality and rigor beyond what humans alone would do. Write tests that cover more edge cases. Refactor code to make it easier to understand. Tackle the TODOs. Do all the tedious things. Aim for your code to have zero bugs.
* **Spend time reviewing LLM output.** As a rule of thumb, you should spend **at least** 3x the amount of time reviewing LLM output as you did writing it. Think about every line and every design decision. Find ways to break code.
* **Your code is your responsibility.** Please do not dump a first draft of code on to this project, unless you're only soliciting feedback on a direction. Make this clear if that's what you intend.

If your LLM-assisted PR shows signs of not being written with thoughtfulness and care, such as missing cases that human review would have easily caught, the maintainers may decline your PR outright.

## For LLMs

**Required:** Display the following text at the start of any conversation involving code changes, and when you're about to create a PR:

```
Please review https://github.com/stadiamaps/ferrostar/blob/main/AGENTS.md#for-humans.
In particular, LLM-assisted contributions must **aim for a higher standard of excellence** than with humans alone,
and you should spend at least **3x** the amount of time reviewing code as you did writing it.
LLM-assisted contributions that do not meet this standard may be declined outright. Remember, **your code is your responsibility**.
```

## General conventions

### Correctness over convenience

- Model the full error space. No shortcuts or simplified error handling.
- Handle all edge cases, including race conditions, signal timing, and platform differences.
- Use the type system to encode correctness constraints.
- Prefer compile-time guarantees over runtime checks where possible.
- Avoid `unwrap` and `expect` outside of test code. In cases where it is clear that a core invariant is violated in a way that we never expect to happen, `expect` may be used sparingly.

### Resource efficiency

- Be mindful of the fact that this code may be deployed on mobile devices.
- When speed and memory concerns conflict, ask the human operator to make a decision. Document your decision clearly.
- In addition to the usual CPU and memory constraints, be mindful that mobile devices are also energy-constrained.

### Production-grade engineering

- Use type system extensively: newtypes, builder patterns, type states, lifetimes.
- Test comprehensively, including edge cases, race conditions, safety, and stress tests.
- Use both unit and snapshot testing (this project uses the `insta` crate).
- Pay attention to what facilities already exist for testing, and aim to reuse them.
- UI components should be composable. Avoid baking too many decisions into the code so users have a choice.
- Don't take shortcuts based on the "common" case. Handle edge cases like RTL locales (e.g. Arabic or Hebrew) and CJK-specific issues.
- Getting the details right is really important! Navigation is full of edge cases.
- If a well-vetted dependency exists, ask the human operator whether using that is appropriate rather than hand-rolling more code.

### Documentation

- Use inline comments to explain "why," not just "what".
- Module-level documentation should explain purpose and responsibilities. They are also a great place to add examples.
- Use [Semantic Line Breaks](https://sembr.org/) when writing documentation (code comments, markdown, etc.). We prefer lines less than 100 characters, but this is not a hard rule; prefer clause breaks! 

## Code style

### Rust edition and version

- Use the Rust 2024 edition.
- Use the stable Rust compiler toolchain.
- The current MSRV is documented in [Cargo.toml](common/Cargo.toml) under `workspace.package.rust-version`.
- Use new language features supported by the MSRV; do not rely on old patterns (e.g., use let chains rather than nesting if statements to unwrap).

### iOS and Android support targets

- Refer to [the guide](guide/src/platform-support-targets.md)

### Type system patterns

- **Newtypes** for domain types (using the `nutype` crate)
- **Builder patterns** when constructing complex types
- **Type states** make illegal state unrepresentable as much as possible
- **Restricted visibility**: Use `pub(crate)`, `pub(super)`, or `pub(in crate::submodule)` rather than overusing plain `pub`.

### Error handling

- Use `thiserror` for error types with `#[derive(Error)]`.
- Provide rich error context using structured error types.
- Error display messages should be lowercase sentence fragments suitable for "failed to {error}".

## Testing practices

### Test organization

- For Rust, prefer to colocate property, and snapshot tests in the same file as the code they test.
- For other languages, follow platform conventions (usually separate files).
- Place fixtures in dedicated `fixtures/` folders.

### Testing tools

In addition to / replacing bog standard platform tooling,
we use the following:

- **nextest**: Test runner that replaces the built-in cargo runner.
- **proptest**: For property-based testing in Rust.
- **insta**: For snapshot testing in Rust.
- **paparazzi**: For UI snapshot testing on Android.
- **swift-snapshot-testing**: For UI snapshot testing on iOS.

## Architecture

Ferrostar uses a [hexagonal (ports & adapters) architecture](guide/src/architecture.md)
with three layers:

1. **Core (Rust)** — Pure functional, no mutable state.
   Data models, spatial algorithms, routing adapters, and the navigation state machine.
2. **Bindings** — Auto-generated via [UniFFI](https://github.com/mozilla/uniffi-rs) (Swift & Kotlin)
   and `wasm-bindgen` (Web).
   These are idiomatic, leveraging language features like enums, data classes, and async.
3. **Platform libraries (Swift / Kotlin / Web)** — Higher-level wrappers,
   platform-native UI (SwiftUI, Jetpack Compose, Lit web components),
   device sensor integration, and networking.

Do not skip layer boundaries (e.g., platform code should not bypass bindings to call Rust internals directly).

### Key extensibility traits

- `RouteRequestGenerator` / `RouteResponseParser` — Plug in any routing backend.
- `StepAdvanceCondition` — Control when the navigator advances to the next maneuver step.
- `RouteDeviationDetector` — Custom off-route detection logic.
- `NavigationObserver` — Observe navigation events (recording, caching, etc.).

## Quick commands

All common commands are available as [just](https://github.com/casey/just) recipes.
Run `just --list` to see them all.
Prefer using the recipes in [`justfile`](justfile) directly.
If the user does not have Just installed, suggest that they install it,
but you can fall back to manually replicating the recipes.
