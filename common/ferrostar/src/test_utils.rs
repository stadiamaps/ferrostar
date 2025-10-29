use crate::models::{GeographicCoordinate, UserLocation};
use geo::{coord, Coord};
use proptest::prop_compose;

use insta::_macro_support::Content;
use insta::internals::ContentPath;
use serde::de::DeserializeOwned;
use serde::Serialize;
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

/// An insta redaction that parses property bytes as a generic type and returns a JSON string.
///
/// This enables both validation and easier diffing.
pub fn redact_properties<T: DeserializeOwned + Serialize>(
    value: Content,
    _path: ContentPath,
) -> String {
    // Deserialize to properties (so we know it's in the right format!)
    let content_slice = value.as_slice().expect("Unable to get content as slice");
    let content_bytes: Vec<_> = content_slice
        .iter()
        .map(|c| {
            let c64 = c.as_u64().expect("Could not get content value as a number");
            u8::try_from(c64).expect("Unexpected byte value")
        })
        .collect();
    let result: T = serde_json::from_slice(&content_bytes)
        .expect("Unable to deserialize as OsrmWaypointProperties");
    serde_json::to_string(&result).expect("Unable to serialize")
}
