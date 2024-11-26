use crate::models::{BoundingBox, GeographicCoordinate, Route, RouteStep, Waypoint, WaypointKind};
use crate::routing_adapters::{osrm::OsrmResponseParser, RouteResponseParser};
#[cfg(feature = "alloc")]
use alloc::string::ToString;
use geo::{line_string, BoundingRect, Haversine, Length, LineString, Point};

// A more complex route
const VALHALLA_EXTENDED_OSRM_RESPONSE: &str = r#"{"routes":[{"weight_name":"auto","weight":462.665,"duration":182.357,"distance":1718.205,"legs":[{"via_waypoints":[],"admins":[{"iso_3166_1_alpha3":"USA","iso_3166_1":"US"}],"weight":462.665,"duration":182.357,"steps":[{"bannerInstructions":[{"primary":{"type":"end of road","modifier":"right","text":"John F. Kennedy Boulevard","components":[{"text":"John F. Kennedy Boulevard","type":"text"},{"text":"/","type":"delimiter"},{"text":"CR 501","type":"text"}]},"distanceAlongGeometry":64.13}],"intersections":[{"classes":["restricted"],"entry":[true],"bearings":[151],"duration":16.247,"admin_index":0,"out":0,"weight":18.684,"geometry_index":0,"location":[-74.031614,40.775707]},{"entry":[false,true,false,false],"classes":["restricted"],"in":3,"bearings":[121,175,239,331],"duration":3.995,"turn_weight":15,"turn_duration":0.035,"admin_index":0,"out":1,"weight":19.554,"geometry_index":1,"location":[-74.031354,40.775349]},{"bearings":[63,159,244,355],"entry":[false,true,false,false],"classes":["restricted"],"in":3,"turn_weight":15,"turn_duration":0.061,"admin_index":0,"out":1,"geometry_index":2,"location":[-74.031343,40.775254]}],"maneuver":{"instruction":"Drive southeast.","type":"depart","bearing_after":151,"bearing_before":0,"location":[-74.031614,40.775707]},"name":"","duration":23.182,"distance":64.13,"driving_side":"right","weight":56.55,"mode":"driving","geometry":"u`wwlAz~oelCjUgO|DU|B_A"},{"bannerInstructions":[{"primary":{"type":"on ramp","modifier":"slight left","text":"Take the ramp on the left.","components":[{"text":"Take the ramp on the left.","type":"text"}]},"distanceAlongGeometry":115}],"intersections":[{"entry":[false,true,false],"in":2,"bearings":[63,252,339],"duration":5.392,"turn_weight":20,"turn_duration":2.423,"admin_index":0,"out":1,"weight":23.414,"geometry_index":3,"location":[-74.031311,40.775191]},{"entry":[false,true,true,true],"in":0,"bearings":[99,144,282,328],"duration":4.598,"turn_weight":10,"lanes":[{"indications":["left"],"valid":false,"active":false},{"indications":["straight"],"valid_indication":"straight","valid":true,"active":true},{"indications":["straight"],"valid_indication":"straight","valid":true,"active":false}],"turn_duration":2.008,"admin_index":0,"out":2,"weight":12.978,"geometry_index":9,"location":[-74.031856,40.775165]},{"bearings":[48,94,269],"entry":[false,false,true],"in":1,"turn_weight":2.5,"turn_duration":0.026,"admin_index":0,"out":2,"geometry_index":12,"location":[-74.032336,40.775218]}],"maneuver":{"modifier":"right","instruction":"Turn right onto John F. Kennedy Boulevard/CR 501.","type":"end of road","bearing_after":252,"bearing_before":159,"location":[-74.031311,40.775191]},"name":"John F. Kennedy Boulevard","duration":12.446,"distance":115,"driving_side":"right","weight":41.686,"mode":"driving","ref":"CR 501","geometry":"m`vwlA|koelCl@fCj@lC\\zEUfHQzC[jCgApJ[dJEfFFjS"},{"bannerInstructions":[{"primary":{"type":"fork","modifier":"slight right","text":"NJ 495 West, NJTP West","components":[{"text":"NJ 495 West, NJTP West","type":"text"},{"text":"/","type":"delimiter"},{"text":"NJ 495","type":"text"}]},"distanceAlongGeometry":236}],"intersections":[{"entry":[false,true,true],"in":0,"bearings":[89,249,265],"duration":17.813,"turn_duration":0.083,"admin_index":0,"out":1,"weight":20.39,"geometry_index":13,"location":[-74.032662,40.775214]},{"bearings":[37,237],"entry":[false,true],"in":0,"admin_index":0,"out":1,"geometry_index":26,"location":[-74.034357,40.77406]}],"maneuver":{"modifier":"slight left","instruction":"Take the ramp on the left.","type":"on ramp","bearing_after":249,"bearing_before":269,"location":[-74.032662,40.775214]},"name":"","duration":21.323,"distance":236,"driving_side":"right","weight":24.514,"mode":"driving","geometry":"{avwlAj`relCrBbJVvBXvBh@pCh@dCh@tBj@lBxB~FrBdErCrEhC`DjCrCzg@|g@v@fAdAlBn@fC\\fBPfEJzE"},{"intersections":[{"entry":[false,false,true,true],"classes":["motorway"],"in":0,"bearings":[82,120,260,300],"duration":10.631,"turn_weight":2.1,"turn_duration":0.088,"admin_index":0,"out":3,"weight":14.488,"geometry_index":32,"location":[-74.034778,40.773943]},{"entry":[false,false,true],"classes":["motorway"],"in":1,"bearings":[108,114,290],"duration":0.924,"turn_duration":0.024,"admin_index":0,"out":2,"weight":1.057,"geometry_index":43,"location":[-74.037391,40.774928]},{"entry":[true,false,true],"classes":["motorway"],"in":1,"bearings":[27,110,289],"duration":4.905,"turn_duration":0.019,"admin_index":0,"out":2,"weight":5.741,"geometry_index":44,"location":[-74.037621,40.774991]},{"entry":[false,false,true],"classes":["motorway"],"in":0,"bearings":[100,114,295],"duration":0.65,"turn_weight":30.2,"turn_duration":0.02,"admin_index":0,"out":2,"weight":30.94,"geometry_index":47,"location":[-74.03891,40.775288]},{"entry":[false,true,true],"classes":["motorway"],"in":0,"bearings":[115,296,318],"duration":1.087,"lanes":[{"indications":["straight"],"valid_indication":"straight","valid":true,"active":false},{"indications":["straight"],"valid_indication":"straight","valid":true,"active":false},{"indications":["straight"],"valid_indication":"straight","valid":true,"active":false},{"indications":["straight","slight right"],"valid_indication":"straight","valid":true,"active":true}],"turn_duration":0.007,"admin_index":0,"out":1,"weight":1.269,"geometry_index":48,"location":[-74.039057,40.775341]},{"bearings":[116,296],"entry":[false,true],"classes":["motorway"],"in":0,"admin_index":0,"out":1,"geometry_index":49,"location":[-74.039315,40.775435]}],"bannerInstructions":[{"secondary":{"text":"US 1 South, US 9 South: Jersey City","components":[{"text":"US 1 South, US 9 South: Jersey City","type":"text"}]},"primary":{"type":"off ramp","modifier":"slight right","text":"Tonnelle Avenue","components":[{"text":"Tonnelle Avenue","type":"text"},{"text":"/","type":"delimiter"},{"text":"US 1; US 9","type":"text"}]},"distanceAlongGeometry":558},{"distanceAlongGeometry":400,"primary":{"type":"off ramp","modifier":"slight right","text":"Tonnelle Avenue","components":[{"text":"Tonnelle Avenue","type":"text"},{"text":"/","type":"delimiter"},{"text":"US 1; US 9","type":"text"}]},"secondary":{"text":"US 1 South, US 9 South: Jersey City","components":[{"text":"US 1 South, US 9 South: Jersey City","type":"text"}]},"sub":{"text":"","components":[{"active":false,"text":"","directions":["straight"],"type":"lane"},{"active":false,"text":"","directions":["straight"],"type":"lane"},{"active":false,"text":"","directions":["straight"],"type":"lane"},{"active_direction":"right","active":true,"text":"","directions":["straight","right"],"type":"lane"}]}}],"destinations":"NJ 495 West, NJTP West","maneuver":{"modifier":"slight right","instruction":"Keep right to take NJ 495 West/NJTP West.","type":"fork","bearing_after":300,"bearing_before":262,"location":[-74.034778,40.773943]},"name":"","duration":24.452,"distance":558,"driving_side":"right","weight":60.845,"mode":"driving","ref":"NJ 495","geometry":"mrswlArdvelCqNnb@{CbJoBpGqBvGmB`HyBjIwBpImBvH}AzGwE~S}Hv\\}BjMqKpn@}AtIaBhUiBdH{DbOo`@t{A"},{"intersections":[{"entry":[false,true,true],"in":0,"bearings":[116,296,313],"duration":25.673,"lanes":[{"indications":["straight"],"valid":false,"active":false},{"indications":["straight"],"valid":false,"active":false},{"indications":["straight"],"valid":false,"active":false},{"indications":["straight","right"],"valid_indication":"right","valid":true,"active":true}],"turn_duration":0.023,"admin_index":0,"out":2,"weight":30.139,"geometry_index":50,"location":[-74.040798,40.775971]},{"entry":[true,false],"in":1,"bearings":[172,323],"duration":10.463,"admin_index":0,"out":0,"weight":12.293,"geometry_index":90,"location":[-74.040181,40.776747]},{"bearings":[18,30,207],"entry":[false,true,true],"in":0,"turn_weight":27.4,"turn_duration":0.013,"admin_index":0,"out":2,"geometry_index":101,"location":[-74.040403,40.775953]}],"bannerInstructions":[{"primary":{"type":"turn","modifier":"slight right","text":"29th Street","components":[{"text":"29th Street","type":"text"}]},"distanceAlongGeometry":372}],"destinations":"US 1 South, US 9 South: Jersey City","maneuver":{"modifier":"slight right","instruction":"Take the US 1 South/US 9 South exit toward Jersey City.","type":"off ramp","bearing_after":313,"bearing_before":296,"location":[-74.040798,40.775971]},"name":"Tonnelle Avenue","duration":38.663,"distance":372,"driving_side":"right","weight":72.787,"mode":"driving","ref":"US 1; US 9","geometry":"eqwwlAz|aflC_LnQiBvCwAlBuArAeAv@mAp@sAh@sA^kATgAJeAAyAIgAIeAWkAc@mAo@cAs@kAgAcAgAy@{A{@cBm@wAk@gBc@sBY{BOsBGuBA_CBcBP{BZwB\\uAl@wBr@aBr@sA|@wAz@gA~@aAlA_AlAs@z@YlAY`ASz@K|@CdADjAN~@VpAj@lDfBzWpIrXbP"},{"bannerInstructions":[{"primary":{"type":"new name","modifier":"right","text":"Dell Avenue","components":[{"text":"Dell Avenue","type":"text"}]},"distanceAlongGeometry":84}],"intersections":[{"entry":[false,true,true],"in":0,"bearings":[27,207,249],"duration":5.755,"turn_weight":11.3,"turn_duration":0.115,"admin_index":0,"out":2,"weight":17.927,"geometry_index":102,"location":[-74.040677,40.775543]},{"entry":[false,true,true],"in":0,"bearings":[106,128,293],"duration":0.371,"turn_weight":4.2,"turn_duration":0.011,"admin_index":0,"out":2,"weight":4.623,"geometry_index":108,"location":[-74.041213,40.775524]},{"bearings":[113,201,297],"entry":[false,true,true],"in":0,"turn_weight":4.2,"turn_duration":0.009,"admin_index":0,"out":2,"geometry_index":109,"location":[-74.04125,40.775536]}],"maneuver":{"modifier":"slight right","instruction":"Bear right onto 29th Street.","type":"turn","bearing_after":249,"bearing_before":207,"location":[-74.040677,40.775543]},"name":"29th Street","duration":10.215,"distance":84,"driving_side":"right","weight":31.544,"mode":"driving","geometry":"mvvwlAhuaflCvAfHXjBLnB?`C[nC}@xGWhAqGjU"},{"bannerInstructions":[{"primary":{"type":"arrive","modifier":"left","text":"Your destination is on the left.","components":[{"text":"Your destination is on the left.","type":"text"}]},"distanceAlongGeometry":289.074}],"intersections":[{"entry":[true,false],"in":1,"bearings":[27,117],"duration":4.14,"turn_weight":88.4,"admin_index":0,"out":0,"weight":93.264,"geometry_index":110,"location":[-74.041608,40.775673]},{"entry":[true,true,false],"in":2,"bearings":[27,115,207],"duration":13.507,"turn_weight":4.2,"turn_duration":0.007,"admin_index":0,"out":0,"weight":20.062,"geometry_index":111,"location":[-74.041485,40.775855]},{"entry":[true,true,false],"in":2,"bearings":[27,115,207],"duration":0.547,"turn_weight":4.2,"turn_duration":0.007,"admin_index":0,"out":0,"weight":4.835,"geometry_index":112,"location":[-74.041079,40.776457]},{"entry":[true,false,true],"in":1,"bearings":[27,207,303],"duration":13.687,"turn_weight":4.2,"turn_duration":0.007,"admin_index":0,"out":0,"weight":20.274,"geometry_index":113,"location":[-74.041062,40.776482]},{"entry":[true,false,true],"in":1,"bearings":[27,207,296],"duration":12.607,"turn_weight":4.2,"turn_duration":0.007,"admin_index":0,"out":0,"weight":19.005,"geometry_index":114,"location":[-74.040653,40.777088]},{"entry":[true,true,false],"in":2,"bearings":[27,115,207],"duration":5.767,"turn_weight":4.2,"turn_duration":0.007,"admin_index":0,"out":0,"weight":10.968,"geometry_index":115,"location":[-74.040274,40.777651]},{"bearings":[27,115,207],"entry":[true,true,false],"in":2,"turn_weight":4.2,"turn_duration":0.007,"admin_index":0,"out":0,"geometry_index":116,"location":[-74.040103,40.777904]}],"maneuver":{"modifier":"right","instruction":"Turn right onto Dell Avenue.","type":"new name","bearing_after":27,"bearing_before":297,"location":[-74.041608,40.775673]},"name":"Dell Avenue","duration":52.075,"distance":289.074,"driving_side":"right","weight":174.739,"mode":"driving","geometry":"q~vwlAnocflCkJuFsd@kXq@a@{d@qXeb@uVyNuIaDmB"},{"intersections":[{"bearings":[207],"entry":[true],"in":0,"admin_index":0,"geometry_index":117,"location":[-74.040048,40.777985]}],"bannerInstructions":[],"maneuver":{"modifier":"left","instruction":"Your destination is on the left.","type":"arrive","bearing_after":0,"bearing_before":27,"location":[-74.040048,40.777985]},"name":"Dell Avenue","duration":0,"distance":0,"driving_side":"right","weight":0,"mode":"driving","geometry":"ao{wlA~m`flC??"}],"distance":1718.205,"summary":"NJ 495, US 1"}],"geometry":"u`wwlAz~oelCjUgO|DU|B_Al@fCj@lC\\zEUfHQzC[jCgApJ[dJEfFFjSrBbJVvBXvBh@pCh@dCh@tBj@lBxB~FrBdErCrEhC`DjCrCzg@|g@v@fAdAlBn@fC\\fBPfEJzEqNnb@{CbJoBpGqBvGmB`HyBjIwBpImBvH}AzGwE~S}Hv\\}BjMqKpn@}AtIaBhUiBdH{DbOo`@t{A_LnQiBvCwAlBuArAeAv@mAp@sAh@sA^kATgAJeAAyAIgAIeAWkAc@mAo@cAs@kAgAcAgAy@{A{@cBm@wAk@gBc@sBY{BOsBGuBA_CBcBP{BZwB\\uAl@wBr@aBr@sA|@wAz@gA~@aAlA_AlAs@z@YlAY`ASz@K|@CdADjAN~@VpAj@lDfBzWpIrXbPvAfHXjBLnB?`C[nC}@xGWhAqGjUkJuFsd@kXq@a@{d@qXeb@uVyNuIaDmB"}],"waypoints":[{"distance":0.446,"name":"","location":[-74.031614,40.775707]},{"distance":20.629,"name":"Dell Avenue","location":[-74.040048,40.777985]}],"code":"Ok"}"#;

/// Gets a complex route
///
/// The accuracy of each parser is tested separately in the routing_adapters module;
/// this function simply intends to return a route for an extended test.
pub fn get_extended_route() -> Route {
    let parser = OsrmResponseParser::new(6);
    parser
        .parse_response(VALHALLA_EXTENDED_OSRM_RESPONSE.into())
        .expect("Unable to parse OSRM response")
        .pop()
        .expect("Expected a route")
}

pub fn gen_dummy_route_step(
    start_lng: f64,
    start_lat: f64,
    end_lng: f64,
    end_lat: f64,
) -> RouteStep {
    RouteStep {
        geometry: vec![
            GeographicCoordinate {
                lng: start_lng,
                lat: start_lat,
            },
            GeographicCoordinate {
                lng: end_lng,
                lat: end_lat,
            },
        ],
        distance: line_string![
            (x: start_lng, y: start_lat),
            (x: end_lng, y: end_lat)
        ]
        .length::<Haversine>(),
        duration: 0.0,
        road_name: None,
        instruction: "".to_string(),
        visual_instructions: vec![],
        spoken_instructions: vec![],
        annotations: None,
        incidents: vec![],
    }
}

pub fn gen_route_from_steps(steps: Vec<RouteStep>) -> Route {
    let geometry: Vec<_> = steps
        .iter()
        .flat_map(|step| step.geometry.clone())
        .collect();
    let linestring = LineString::from_iter(geometry.iter().map(|point| Point::from(*point)));
    let distance = steps.iter().fold(0.0, |acc, step| acc + step.distance);
    let bbox = linestring.bounding_rect().unwrap();

    Route {
        geometry,
        bbox: BoundingBox {
            sw: GeographicCoordinate::from(bbox.min()),
            ne: GeographicCoordinate::from(bbox.max()),
        },
        distance,
        waypoints: vec![
            // This method cannot be used outside the test configuration,
            // so unwraps are OK.
            Waypoint {
                coordinate: steps.first().unwrap().geometry.first().cloned().unwrap(),
                kind: WaypointKind::Break,
            },
            Waypoint {
                coordinate: steps.last().unwrap().geometry.last().cloned().unwrap(),
                kind: WaypointKind::Break,
            },
        ],
        steps,
    }
}
