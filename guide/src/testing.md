# Testing

We employ a mix of testing tools and methodologies across the Ferrostar stack.
We use a mix of unit, integration, snapshot, and property testing.
When possible, please include tests in your PRs.
If you can employ multiple strategies
(ex: unit testing + property testing),
please do!

Tests are automatically run as part of CI.

## Types of tests

Unit tests typically verify that some function gives an expected output
for some known inputs.
This is great for verifying specific properties
of pure functions.
For example, checking that 1+1 = 2.
However, just because you checked a few examples doesn’t tell us that the code is correct.
Despite this limitation, unit tests are a great tool, and are very fast to run.
Add them where possible, as they complement other strategies.

Property testing lets you specify some random variables
(possibly with limits; ex: floating point numbers between -180 and 180)
rather than static inputs.
This lets you test *invariants* rather than specific cases.
For example, asserting that *any* integer plus zero equals itself.
Property testing is a great fit for highly “algorithmic” code,
and we use it extensively.

Snapshot testing executes some code
and then takes a “snapshot” of the state.
This can be applied to UI code,
where an image snapshot of a view is saved after rendering.
We use this extensively for ensuring that overlays render correctly
with static inputs, for example.
It can also be applied to arbitrary data structures.
We use snapshot tests to test things like
“given a fixed route and a stream of GPS updates,
what do all the intermediate state transitions look like?”

Finally, integration testing is similar to unit testing
in that the inputs are static.
However, it’s designed to test much more of a system “end to end”
whereas unit tests are usually targeted at a single function.

All of the approaches above combine to give us great confidence
in the correctness of Ferrostar’s core business logic,
and let us refactor without fear of breakage.

Let’s look at the specific tooling for each platform.

## Rust

On Rust, we employ both unit and integration tests using the standard cargo tooling.
In addition, we employ both property testing via [`proptest`](https://crates.io/crates/proptest)
and snapshot testing via [`insta`](https://crates.io/crates/cargo-insta).

There is nothing special about running unit, integration, and property tests;
just run `cargo test`!
For snapshot tests, when a snapshot does not exist (new test)
or the test output doesn’t match the stored snapshot,
you’ll get a test error.
You can review the changes using `cargo insta review`,
a CLI tool which shows a colorized diff and prompts whether to update the snapshot.
Snapshot files are committed to the repo and will show in the PR diffs.

## iOS

iOS testing is best done in Xcode, but you can also use the terminal
(see the github actions for commands we run).
Open up Xcode and press cmd+u to run tests.

Most of the tests are just regular XCUnit tests that you’re already used to.
Of note, we include some snapshot testing via macros
which snapshot the views.
When adding a new snapshot test or changing a view,
you’ll get an error.
Modify the snapshot assertion function to include the keyword argument
`record: true` to update the snapshot.
This will be committed to git and visible in diffs.

## Android

On Android, we use a mix of standard JUnit unit tests (fast),
snapshot tests with Paparazzi (pretty fast),
and connected checks (SLOW).
You can invoke tests with `./gradlew test`,
`./gradlew verifyPaparazziDebug`,
and `./gradlew connectedCheck` respectively.

To record snapshots for a new test or update old ones,
run `./gradlew recordPaparazziDebug`.

## Web

TBD