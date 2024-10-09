# Rust

Not using a platform listed / building an embedded application?
You're in the right spot!

Ferrostar is built in Rust,
which makes it portable to a wide range of OS and CPU combinations.
The core includes common data models, traits, common routing backend integrations,
and more.

The core documentation is hosted, like every crate, on [docs.rs](https://docs.rs/ferrostar/latest).

The core of a custom navigation experience is the [`NavigationController`](https://docs.rs/ferrostar/latest/ferrostar/navigation_controller/struct.NavigationController.html).
The controller is initialized with a route and configuration.

You can either construct a route yourself manually,
or use some of the existing tooling to get started.
Unlike the higher level platforms like iOS and Swift,
no high-level core wrapper handles HTTP for you (to keep the core light),
but you can add your own using a crate like `reqwest`,
or you could run an embedded routing engine for offline routing.
If your routing API uses a common response format like OSRM,
[check out the included parsers](https://docs.rs/ferrostar/latest/ferrostar/routing_adapters/index.html).

The `NavigationController` is pure (in a functional sense),
and it is up to integrators to decide on the most appropriate state storage mechanism.
Use the `get_initial_state` function to create an initial state with the user’s location.
Then, as new location updates arrive, or you decide to manually advance to the next step,
call the appropriate methods.
Each method call returns a new `TripState`,
which you can store and take any actions on
(such as updating the UI or deciding to recalculate the route).

At a high level, that’s pretty much it!
The Ferrostar core includes most of the proverbial legos;
the rest is up to you.