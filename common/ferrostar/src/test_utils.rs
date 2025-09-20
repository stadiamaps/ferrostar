use crate::models::{GeographicCoordinate, UserLocation};
use geo::{coord, Coord};
use proptest::prop_compose;

#[cfg(all(feature = "std", not(feature = "web-time")))]
use std::time::SystemTime;

#[cfg(feature = "web-time")]
use web_time::SystemTime;

pub fn make_user_location(coord: Coord, horizontal_accuracy: f64) -> UserLocation {
    UserLocation {
        coordinates: GeographicCoordinate {
            lat: coord.y,
            lng: coord.x,
        },
        horizontal_accuracy,
        course_over_ground: None,
        timestamp: SystemTime::now(),
        speed: None,
    }
}

prop_compose! {
    pub fn arb_coord()(x in -180f64..180f64, y in -90f64..90f64) -> Coord {
        coord! {x: x, y: y}
    }
}

prop_compose! {
    pub fn arb_user_loc(horizontal_accuracy: f64)(coord in arb_coord()) -> UserLocation {
        make_user_location(coord, horizontal_accuracy)
    }
}
