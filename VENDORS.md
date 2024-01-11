# Commercial Vendors

Here is a list of commercial vendors with APIs that are currently supported by Ferrostar.
PRs welcome to expand the list (or to add support, of course!).

## Routing

* [Stadia Maps](https://stadiamaps.com/) (Valhalla; hosted API)
* [GIS • OPS](https://gis-ops.com/) (Valhalla; custom deployments and integration of proprietary data)

NOTE: It is fairly straightforward to support Mapbox as well,
since their responses are compatible with Valhalla's modified OSRM format.
Someone just needs to write a request adapter (PRs welcome; it's not that hard).

## Basemaps

You can use basemaps from any vendor that works with MapLibre.
Here are a few popular ones:

* [Stadia Maps](https://stadiamaps.com/)
* [Mapbox](https://mapbox.com/)
* [Jawg](https://www.jawg.io/)
* [MapTiler](https://maptiler.com/)
