# Getting Started on the Web

This section of the guide covers how to integrate Ferrostar into a web app.
While there are limitations to the web [Geolocation API](https://developer.mozilla.org/en-US/docs/Web/API/Geolocation_API)
(notably no background updates),
PWAs and other mobile-optimized sites
can be a great solution when a native iOS/Android app is impractical or prohibitively expensive.

We'll cover the "batteries included" approach, but flag areas for customization and overrides along the way.

## Add the package dependency

### Installing with `npm`

NOTE: Currently you need to build the package locally.
We intend to publish to npmjs.com very soon.

In your web app, you can add the Ferrostar NPM package as a dependency.
You will need to install [Rust](https://www.rust-lang.org/) and `wasm-pack` to build the NPM package.

```shell
cargo install wasm-pack
```

Head to the local path where you have checked out Ferrostar,
go to the `web` directory, and build the module:

```shell
npm install && npm run build
```

Then, in your project, install the Ferrostar package using the local path:

```shell
npm install /path/to/ferrostar/web
```

### Using unpkg

TODO after publishing to npm.

## Add Ferrostar web components to your web app

The Ferrostar web SDK uses the [Web Components](https://developer.mozilla.org/en-US/docs/Web/API/Web_components)
to ensure maximum compatibility across frontend frameworks.
You can import the components just like other things you’re used to in JavaScript.

```javascript
import { FerrostarCore, BrowserLocationProvider } from "ferrostar-components";
```

## Configure the `<ferrostar-core>` component

Now you can use Ferrostar in your HTML like this:

```html
<ferrostar-core
  id="core"
  valhallaEndpointUrl="https://api.stadiamaps.com/route/v1"
  styleUrl="https://tiles.stadiamaps.com/styles/outdoors.json"
  profile="bicycle"
></ferrostar-core>
```

Here we have used Stadia Maps URLs, which should work without authentication for local development.
(Refer to the [authentication docs](https://docs.stadiamaps.com/authentication/)
for network deployment details; you can start with a free account.)

See the [vendors appendix](./vendors.md) for a list of other compatible vendors.

`<ferrostar-core>`  additionally requires setting some CSS manually, or it will be invisible!

```css
ferrostar-core {
  display: block;
  width: 100%;
  height: 100%;
}
```

That’s all you need to get started!

### Configuration explained

`<ferrostar-core>` provides a few properties to configure.
Here are the most important ones:

- `valhallaEndpointUrl`: The Valhalla routing endpoint to use. You can use any reasonably up-to-date Valhalla server, including your own. See [vendors](./vendor.md#routing) for a list of known compatible vendors.
- `httpClient`: You can set your own fetch-compatible HTTP client to make requests to the routing API (ex: Valhalla).
- `costingOptions`: You can set the costing options for the route provider (ex: Valhalla JSON options).
- `useIntegratedSearchBox`: Ferrostar web includes a search box powered by Stadia Maps, but you can disable this and replace with your own.

NOTE: The JavaScript API is currently limited to Valhalla,
but support for arbitrary providers (like we already have on iOS and Android)
is [tracked in this issue](https://github.com/stadiamaps/ferrostar/issues/191).

## Demo app

We've put together a minimal demo app with an example integration.
Check out the [source code](https://github.com/stadiamaps/ferrostar/tree/main/web/index.html)
or try the [hosted demo](https://stadiamaps.github.io/ferrostar/web-demo)
(works best from a phone if you want to use real geolocation).

## Going deeper

This covers the basic “batteries included” configuration and pre-built UI.
But there’s a lot of room for customization!
Skip on over to the customization chapters that interest you.