# Jetpack Compose

## Customizing the instruction banners

Ferrostar includes a number of views related to instruction banners.
These are composed together to provide sensible defaults,
but you can customize a number of things.

### Distance formatting

By default, banners and other UI elements involving distance will be formatted using the bundled `com.stadiamaps.ferrostar.composeui.LocalizedDistanceFormatter`.
Distance formatting is a complex topic though, and there are ways it can go wrong.

The Android ecosystem unfortunately does not include a generalized distance formatter,
so we have to roll our own.
Java locale does not directly specify which units should be preferred for a class of measurement.

We attempt to infer the correct measurement system to use,
using some newer Android APIs.
Unfortunately, Android doesn’t really have a facility for specifying measurement system
independently of the language and region setting.
So, we allow overrides in our default implementation.

If this isn’t enough, you can implement your own formatter
by implementing the `com.stadiamaps.ferrostar.composeui.DistanceFormatter` interface.

If you find an edge case, please file a bug report (and PR if possible)!
