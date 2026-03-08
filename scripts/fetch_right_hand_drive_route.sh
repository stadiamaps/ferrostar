# Set the STADIAMAPS_API_KEY environment variable and use this to fetch Valhalla OSRM formatted
# routes. These are useful for test fixtures, etc.

# A small route with multiple roundabouts and RHD (Manchester UK)
curl -X POST -H "Content-Type: application/json" -d '{
    "locations": [
        {
            "lon": -2.273519,
            "lat": 53.486540
        },
        {
            "lon": -2.285129,
            "lat": 53.490868
        },
        {
            "lon": -2.275551,
            "lat": 53.496885
        }
    ],
    "format": "osrm",
    "costing": "auto",
    "costing_options": {
        "auto": {
            "use_highways": 0.3
        }
    },
    "units": "miles"
}' "https://api.stadiamaps.com/optimized_route/v1?api_key=${STADIAMAPS_API_KEY}"
