//! Tools for deciding when the user has sufficiently deviated from a route.
//!
//! The types in this module are designed around route deviation detection as a single responsibility:
//!
//! While the most common use for this is triggering route recalculation,
//! the decision to reroute (or display an overlay on screen, or any other action) lies with higher levels.
//!
//! For example, on iOS and Android, the `FerrostarCore` class is in charge of deciding
//! when to kick off a new route request.
//! Similarly, you may observe this in your own UI layer and display an overlay under certain conditions.
//!
//! When architecting a Ferrostar core integration for a new platform,
//! we suggest enforcing a similar separation of concerns.

use crate::algorithms::deviation_from_line;
use crate::models::{Route, RouteStep, UserLocation};
#[cfg(feature = "alloc")]
use alloc::sync::Arc;
use geo::{LineString, Point};
use serde::{Deserialize, Serialize};
#[cfg(feature = "wasm-bindgen")]
use tsify::Tsify;

#[cfg(test)]
use {
    crate::{
        models::GeographicCoordinate,
        navigation_controller::test_helpers::{gen_dummy_route_step, gen_route_from_steps},
    },
    proptest::prelude::*,
};

#[cfg(all(test, feature = "std", not(feature = "web-time")))]
use std::time::SystemTime;

#[cfg(all(test, feature = "web-time"))]
use web_time::SystemTime;

/// Determines if the user has deviated from the expected route.
#[derive(Clone, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(from_wasm_abi))]
pub enum RouteDeviationTracking {
    /// No checks will be done, and we assume the user is always following the route.
    None,
    /// Detects deviation from the route using a configurable static distance threshold from the route line.
    #[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
    StaticThreshold {
        /// The minimum required horizontal accuracy of the user location, in meters.
        /// Values larger than this will not trigger route deviation warnings.
        minimum_horizontal_accuracy: u16,
        /// The maximum acceptable deviation from the route line, in meters.
        ///
        /// If the distance between the reported location and the expected route line
        /// is greater than this threshold, it will be flagged as an off route condition.
        max_acceptable_deviation: f64,
    },
    // TODO: Standard variants that account for mode of travel. For example, `DefaultFor(modeOfTravel: ModeOfTravel)` with sensible defaults for walking, driving, cycling, etc.
    /// An arbitrary user-defined implementation.
    /// You decide with your own [`RouteDeviationDetector`] implementation!
    #[serde(skip)]
    Custom {
        detector: Arc<dyn RouteDeviationDetector>,
    },
}

impl RouteDeviationTracking {
    #[must_use]
    pub(crate) fn check_route_deviation(
        &self,
        location: UserLocation,
        route: &Route,
        current_route_step: &RouteStep,
    ) -> RouteDeviation {
        match self {
            RouteDeviationTracking::None => RouteDeviation::NoDeviation,
            RouteDeviationTracking::StaticThreshold {
                minimum_horizontal_accuracy,
                max_acceptable_deviation,
            } => {
                if location.horizontal_accuracy < f64::from(*minimum_horizontal_accuracy) {
                    // Check if the deviation from the route line is within tolerance,
                    // after sanity checking that the positioning signal is within accuracy tolerance.
                    let step_deviation = self.static_threshold_deviation_from_line(
                        &Point::from(location),
                        &current_route_step.get_linestring(),
                        max_acceptable_deviation,
                    );

                    // Exit early if the step is within tolerance, otherwise check there route line.
                    if step_deviation == RouteDeviation::NoDeviation {
                        RouteDeviation::NoDeviation
                    } else {
                        // Fall back to the route line. This allows us to re-join the route later
                        // for skipping forward if [`StepAdvanceConditions`] allow it.
                        self.static_threshold_deviation_from_line(
                            &Point::from(location),
                            &route.get_linestring(),
                            max_acceptable_deviation,
                        )
                    }
                } else {
                    RouteDeviation::NoDeviation
                }
            }
            RouteDeviationTracking::Custom { detector } => {
                detector.check_route_deviation(location, route.clone(), current_route_step.clone())
            }
        }
    }

    fn static_threshold_deviation_from_line(
        &self,
        point: &Point,
        line: &LineString,
        max_acceptable_deviation: &f64,
    ) -> RouteDeviation {
        deviation_from_line(point, line).map_or(RouteDeviation::NoDeviation, |deviation| {
            if deviation > 0.0 && deviation > *max_acceptable_deviation {
                RouteDeviation::OffRoute {
                    deviation_from_route_line: deviation,
                }
            } else {
                RouteDeviation::NoDeviation
            }
        })
    }
}

/// Status information that describes whether the user is proceeding according to the route or not.
///
/// Note that the name is intentionally a bit generic to allow for expansion of other states.
/// For example, we could conceivably add a "wrong way" status in the future.
#[derive(Debug, Copy, Clone, PartialEq, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub enum RouteDeviation {
    /// The user is proceeding on course within the expected tolerances; everything is normal.
    NoDeviation,
    /// The user is off the expected route.
    #[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
    OffRoute {
        /// The deviation from the route line, in meters.
        deviation_from_route_line: f64,
    },
}

/// A custom deviation detector (for extending the behavior of [`RouteDeviationTracking`]).
///
/// This allows for arbitrarily complex implementations when the provided ones are not enough.
/// For example, detecting that the user is proceeding the wrong direction by keeping a ring buffer
/// of recent locations, or perform local map matching.
#[cfg_attr(feature = "uniffi", uniffi::export(with_foreign))]
pub trait RouteDeviationDetector: Send + Sync {
    /// Determines whether the user is following the route correctly or not.
    ///
    /// NOTE: This function has a single responsibility.
    /// Side-effects like whether to recalculate a route are left to higher levels,
    /// and implementations should only be concerned with determining the facts.
    ///
    /// IMPORTANT: If you are short circuiting [`StepAdvanceCondition`]'s to allow
    /// skipping steps, you must always fall back to checking the deviation from the
    /// full route line.
    #[must_use]
    fn check_route_deviation(
        &self,
        location: UserLocation,
        route: Route,
        current_route_step: RouteStep,
    ) -> RouteDeviation;
}

#[cfg(test)]
proptest! {
    /// Tests [`RouteDeviationTracking::None`] behavior,
    /// which never reports that the user is off route, even when they obviously are.
    #[test]
    fn no_deviation_tracking(
        x1: f64, y1: f64,
        x2: f64, y2: f64,
        x3: f64, y3: f64,
    ) {
        let tracking = RouteDeviationTracking::None;
        let current_route_step = gen_dummy_route_step(x1, y1, x2, y2);
        let route = gen_route_from_steps(vec![current_route_step.clone()]);

        // Set the user location to the start of the route step.
        // This is clearly on the route.
        let user_location_on_route = UserLocation {
            coordinates: GeographicCoordinate {
                lng: x1,
                lat: y1,
            },
            horizontal_accuracy: 0.0,
            course_over_ground: None,
            timestamp: SystemTime::now(),
            speed: None
        };
        prop_assert_eq!(
            tracking.check_route_deviation(user_location_on_route, &route, &current_route_step),
            RouteDeviation::NoDeviation
        );

        // Set the user location to a random value.
        // This may be well off route, but we don't care in this mode.
        let user_location_random = UserLocation {
            coordinates: GeographicCoordinate {
                lng: x3,
                lat: y3,
            },
            horizontal_accuracy: 0.0,
            course_over_ground: None,
            timestamp: SystemTime::now(),
            speed: None
        };
        prop_assert_eq!(
            tracking.check_route_deviation(user_location_random, &route, &current_route_step),
            RouteDeviation::NoDeviation
        );
    }

    /// Implements the same behavior as [`RouteDeviationTracking::None`]
    /// with user-supplied code.
    #[test]
    fn custom_no_deviation_mode(
        x1: f64, y1: f64,
        x2: f64, y2: f64,
        x3: f64, y3: f64,
    ) {
        struct NeverDetector {}

        impl RouteDeviationDetector for NeverDetector {
            fn check_route_deviation(
                &self,
                _location: UserLocation,
                _route: Route,
                _current_route_step: RouteStep,
            ) -> RouteDeviation {
                return RouteDeviation::NoDeviation
            }
        }

        let tracking = RouteDeviationTracking::Custom {
            detector: Arc::new(NeverDetector {})
        };
        let current_route_step = gen_dummy_route_step(x1, y1, x2, y2);
        let route = gen_route_from_steps(vec![current_route_step.clone()]);

        // Set the user location to the start of the route step.
        // This is clearly on the route.
        let user_location_on_route = UserLocation {
            coordinates: GeographicCoordinate {
                lng: x1,
                lat: y1,
            },
            horizontal_accuracy: 0.0,
            course_over_ground: None,
            timestamp: SystemTime::now(),
            speed: None
        };
        prop_assert_eq!(
            tracking.check_route_deviation(user_location_on_route, &route, &current_route_step),
            RouteDeviation::NoDeviation
        );

        // Set the user location to a random value.
        // This may be well off route, but we don't care in this mode.
        let user_location_random = UserLocation {
            coordinates: GeographicCoordinate {
                lng: x3,
                lat: y3,
            },
            horizontal_accuracy: 0.0,
            course_over_ground: None,
            timestamp: SystemTime::now(),
            speed: None
        };
        prop_assert_eq!(
            tracking.check_route_deviation(user_location_random, &route, &current_route_step),
            RouteDeviation::NoDeviation
        );
    }

    /// Custom behavior claiming that the user is always off the route.
    #[test]
    fn custom_always_off_route(
        x1: f64, y1: f64,
        x2: f64, y2: f64,
        x3: f64, y3: f64,
    ) {
        struct NeverDetector {}

        impl RouteDeviationDetector for NeverDetector {
            fn check_route_deviation(
                &self,
                _location: UserLocation,
                _route: Route,
                _current_route_step: RouteStep,
            ) -> RouteDeviation {
                return RouteDeviation::OffRoute {
                    deviation_from_route_line: 7.0
                }
            }
        }

        let tracking = RouteDeviationTracking::Custom {
            detector: Arc::new(NeverDetector {})
        };
        let current_route_step = gen_dummy_route_step(x1, y1, x2, y2);
        let route = gen_route_from_steps(vec![current_route_step.clone()]);

        // Set the user location to the start of the route step.
        // This is clearly on the route.
        let user_location_on_route = UserLocation {
            coordinates: GeographicCoordinate {
                lng: x1,
                lat: y1,
            },
            horizontal_accuracy: 0.0,
            course_over_ground: None,
            timestamp: SystemTime::now(),
            speed: None
        };
        prop_assert_eq!(
            tracking.check_route_deviation(user_location_on_route, &route, &current_route_step),
            RouteDeviation::OffRoute {
                deviation_from_route_line: 7.0
            }
        );

        // Set the user location to a random value.
        // This may be well off route, but we don't care in this mode.
        let user_location_random = UserLocation {
            coordinates: GeographicCoordinate {
                lng: x3,
                lat: y3,
            },
            horizontal_accuracy: 0.0,
            course_over_ground: None,
            timestamp: SystemTime::now(),
            speed: None
        };
        prop_assert_eq!(
            tracking.check_route_deviation(user_location_random, &route, &current_route_step),
            RouteDeviation::OffRoute {
                deviation_from_route_line: 7.0
            }
        );
    }

    /// Tests [`RouteDeviationTracking::StaticThreshold`] behavior,
    /// using [`algorithms::deviation_from_line`](crate::algorithms::deviation_from_line)
    #[test]
    fn static_threshold_oracle_test(
        x1: f64, y1: f64,
        x2: f64, y2: f64,
        x3: f64, y3: f64,
        minimum_horizontal_accuracy: u16,
        horizontal_accuracy: f64,
        max_acceptable_deviation in 0f64..,
    ) {
        let tracking = RouteDeviationTracking::StaticThreshold {
            minimum_horizontal_accuracy,
            max_acceptable_deviation
        };
        let current_route_step = gen_dummy_route_step(x1, y1, x2, y2);
        let route = gen_route_from_steps(vec![current_route_step.clone()]);

        // Set the user location to the start of the route step.
        // This is clearly on the route.
        let user_location_on_route = UserLocation {
            coordinates: GeographicCoordinate {
                lng: x1,
                lat: y1,
            },
            horizontal_accuracy,
            course_over_ground: None,
            timestamp: SystemTime::now(),
            speed: None
        };
        prop_assert_eq!(
            tracking.check_route_deviation(user_location_on_route, &route, &current_route_step),
            RouteDeviation::NoDeviation
        );

        // Set the user location to a random value.
        // This may be well off route. Check the deviation_from_line helper
        // as an oracle.
        let coordinates = GeographicCoordinate {
            lng: x3,
            lat: y3,
        };
        let user_location_random = UserLocation {
            coordinates,
            horizontal_accuracy: 0.0,
            course_over_ground: None,
            timestamp: SystemTime::now(),
            speed: None
        };
        let deviation = deviation_from_line(&Point::from(coordinates), &current_route_step.get_linestring());
        match tracking.check_route_deviation(user_location_random, &route, &current_route_step) {
            RouteDeviation::NoDeviation => {
                if let Some(calculated) = deviation {
                    prop_assert!(calculated <= max_acceptable_deviation);
                }
            }
            RouteDeviation::OffRoute{ deviation_from_route_line } => {
                prop_assert_eq!(
                    deviation_from_route_line,
                    deviation.unwrap()
                );
            }
        }
    }

    /// Tests [`RouteDeviationTracking::StaticThreshold`] behavior
    /// for values which are not accurate enough.
    #[test]
    fn static_threshold_ignores_inaccurate_location_updates(
        x1 in -180f64..=180f64, y1 in -90f64..=90f64,
        x2 in -180f64..=180f64, y2 in -90f64..=90f64,
        x3 in -180f64..=180f64, y3 in -90f64..=90f64,
        horizontal_accuracy in 1u16..,
        max_acceptable_deviation: f64,
    ) {
        let tracking = RouteDeviationTracking::StaticThreshold {
            minimum_horizontal_accuracy: horizontal_accuracy - 1,
            max_acceptable_deviation
        };
        let current_route_step = gen_dummy_route_step(x1, y1, x2, y2);
        let route = gen_route_from_steps(vec![current_route_step.clone()]);

        let coordinates = GeographicCoordinate {
            lng: x3,
            lat: y3,
        };
        let user_location_random = UserLocation {
            coordinates,
            horizontal_accuracy: horizontal_accuracy as f64,
            course_over_ground: None,
            timestamp: SystemTime::now(),
            speed: None
        };
        prop_assert_eq!(
            tracking.check_route_deviation(user_location_random, &route, &current_route_step),
            RouteDeviation::NoDeviation
        );
    }
}
