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
use crate::models::Route;
use crate::navigation_controller::models::TripState;
#[cfg(test)]
use crate::{models::UserLocation, navigation_controller::test_helpers::get_navigating_trip_state};
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
        /// The threshold for returning to on-route state, in meters.
        ///
        /// Must be less than or equal to `max_acceptable_deviation`.
        /// This creates hysteresis to prevent oscillation between on/off route states.
        /// For example, if `max_acceptable_deviation` is 50m and `on_route_threshold` is 40m,
        /// the user must deviate more than 50m to trigger off-route, but must return within
        /// 40m to be considered back on route.
        ///
        /// If not specified or equal to `max_acceptable_deviation`, no hysteresis is applied.
        on_route_threshold: f64,
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
        route: &Route,
        trip_state: &TripState,
    ) -> RouteDeviation {
        match self {
            RouteDeviationTracking::None => RouteDeviation::NoDeviation,
            RouteDeviationTracking::StaticThreshold {
                minimum_horizontal_accuracy,
                max_acceptable_deviation,
                on_route_threshold,
            } => match trip_state {
                TripState::Idle { .. } | TripState::Complete { .. } => RouteDeviation::NoDeviation,
                TripState::Navigating {
                    user_location,
                    remaining_steps,
                    deviation,
                    ..
                } => {
                    if user_location.horizontal_accuracy > f64::from(*minimum_horizontal_accuracy) {
                        return RouteDeviation::NoDeviation;
                    }

                    // Choose threshold based on current state (hysteresis)
                    let threshold = match deviation {
                        RouteDeviation::NoDeviation => *max_acceptable_deviation,
                        RouteDeviation::OffRoute { .. } => *on_route_threshold,
                    };

                    let mut first_step_deviation = None;

                    for (index, step) in remaining_steps.iter().enumerate() {
                        let step_deviation = self.static_threshold_deviation_from_line(
                            &Point::from(*user_location),
                            &step.get_linestring(),
                            // Note: This is always <= max_acceptable_deviation
                            threshold,
                        );

                        if index == 0 {
                            first_step_deviation = Some(step_deviation.clone());
                        }

                        if matches!(step_deviation, RouteDeviation::NoDeviation) {
                            return RouteDeviation::NoDeviation;
                        }
                    }

                    first_step_deviation.unwrap_or(RouteDeviation::NoDeviation)
                }
            },
            RouteDeviationTracking::Custom { detector } => {
                detector.check_route_deviation(route.clone(), trip_state.clone())
            }
        }
    }

    /// Get the `RouteDeviation` status for a given location on a line string.
    /// This can be used with a Route or `RouteStep`.
    fn static_threshold_deviation_from_line(
        &self,
        point: &Point,
        line: &LineString,
        max_acceptable_deviation: f64,
    ) -> RouteDeviation {
        deviation_from_line(point, line).map_or(RouteDeviation::NoDeviation, |deviation| {
            if deviation > 0.0 && deviation > max_acceptable_deviation {
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
    fn check_route_deviation(&self, route: Route, trip_state: TripState) -> RouteDeviation;
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
        let trip_state = get_navigating_trip_state(
            user_location_on_route.clone(),
            vec![current_route_step.clone()],
            vec![],
            RouteDeviation::NoDeviation
        );
        prop_assert_eq!(
            tracking.check_route_deviation(&route, &trip_state),
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
        let trip_state_random = get_navigating_trip_state(
            user_location_random.clone(),
            vec![current_route_step.clone()],
            vec![],
            RouteDeviation::NoDeviation
        );
        prop_assert_eq!(
            tracking.check_route_deviation(&route, &trip_state_random),
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
                _route: Route,
                _trip_state: TripState,
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
        let trip_state_on_route = get_navigating_trip_state(
            user_location_on_route.clone(),
            vec![current_route_step.clone()],
            vec![],
            RouteDeviation::NoDeviation
        );
        prop_assert_eq!(
            tracking.check_route_deviation(&route, &trip_state_on_route),
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
        let trip_state_random = get_navigating_trip_state(
            user_location_random.clone(),
            vec![current_route_step.clone()],
            vec![],
            RouteDeviation::NoDeviation
        );
        prop_assert_eq!(
            tracking.check_route_deviation(&route, &trip_state_random),
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
                _route: Route,
                _trip_state: TripState,
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
        let trip_state_on_route = get_navigating_trip_state(
            user_location_on_route.clone(),
            vec![current_route_step.clone()],
            vec![],
            RouteDeviation::NoDeviation
        );
        prop_assert_eq!(
            tracking.check_route_deviation(&route, &trip_state_on_route),
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
        let trip_state_random = get_navigating_trip_state(
            user_location_random.clone(),
            vec![current_route_step.clone()],
            vec![],
            RouteDeviation::NoDeviation
        );
        prop_assert_eq!(
            tracking.check_route_deviation(&route, &trip_state_random),
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
            max_acceptable_deviation,
            on_route_threshold: max_acceptable_deviation
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
        let trip_state = get_navigating_trip_state(
            user_location_on_route.clone(),
            vec![current_route_step.clone()],
            vec![],
            RouteDeviation::NoDeviation
        );
        prop_assert_eq!(
            tracking.check_route_deviation(&route, &trip_state),
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
        let trip_state_random = get_navigating_trip_state(
            user_location_random.clone(),
            vec![current_route_step.clone()],
            vec![],
            RouteDeviation::NoDeviation
        );
        let deviation = deviation_from_line(&Point::from(coordinates), &current_route_step.get_linestring());
        match tracking.check_route_deviation(&route, &trip_state_random) {
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
            max_acceptable_deviation,
            on_route_threshold: max_acceptable_deviation
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
        let trip_state_random = get_navigating_trip_state(
            user_location_random.clone(),
            vec![current_route_step.clone()],
            vec![],
            RouteDeviation::NoDeviation
        );
        prop_assert_eq!(
            tracking.check_route_deviation(&route, &trip_state_random),
            RouteDeviation::NoDeviation
        );
    }
}

/// Tests that hysteresis prevents oscillation between on-route and off-route states.
/// This test verifies that:
/// 1. User goes off-route when deviation > max_acceptable_deviation
/// 2. User stays off-route until deviation <= on_route_threshold
/// 3. User stays on-route until deviation > max_acceptable_deviation again
#[cfg(test)]
#[test]
fn static_threshold_hysteresis_prevents_oscillation() {
    use crate::{
        models::{GeographicCoordinate, UserLocation},
        navigation_controller::test_helpers::{gen_dummy_route_step, gen_route_from_steps, get_navigating_trip_state},
    };

    #[cfg(feature = "std")]
    use std::time::SystemTime;
    #[cfg(feature = "web-time")]
    use web_time::SystemTime;

    let max_deviation = 50.0;
    let on_route_threshold = 40.0;

    let tracking = RouteDeviationTracking::StaticThreshold {
        minimum_horizontal_accuracy: 100,
        max_acceptable_deviation: max_deviation,
        on_route_threshold,
    };

    // Create a simple route step from (0, 0) to (0, 0.001) (~111 meters north)
    let current_route_step = gen_dummy_route_step(0.0, 0.0, 0.0, 0.001);
    let route = gen_route_from_steps(vec![current_route_step.clone()]);

    // Start on route
    let user_location_on_route = UserLocation {
        coordinates: GeographicCoordinate { lng: 0.0, lat: 0.0 },
        horizontal_accuracy: 5.0,
        course_over_ground: None,
        timestamp: SystemTime::now(),
        speed: None,
    };

    // Initially on route
    let trip_state_on_route = get_navigating_trip_state(
        user_location_on_route.clone(),
        vec![current_route_step.clone()],
        vec![],
        RouteDeviation::NoDeviation,
    );

    assert_eq!(
        tracking.check_route_deviation(&route, &trip_state_on_route),
        RouteDeviation::NoDeviation
    );

    // Move 45m away from route (between on_route_threshold and max_deviation)
    // At this distance, should still be on-route since we started on-route
    let user_location_45m = UserLocation {
        coordinates: GeographicCoordinate { lng: 0.0004, lat: 0.0 }, // ~45m east
        horizontal_accuracy: 5.0,
        course_over_ground: None,
        timestamp: SystemTime::now(),
        speed: None,
    };

    let trip_state_45m_from_onroute = get_navigating_trip_state(
        user_location_45m.clone(),
        vec![current_route_step.clone()],
        vec![],
        RouteDeviation::NoDeviation, // Still on-route from previous state
    );

    // Should remain on-route because 45m < max_deviation (50m)
    assert_eq!(
        tracking.check_route_deviation(&route, &trip_state_45m_from_onroute),
        RouteDeviation::NoDeviation
    );

    // Move 55m away from route (beyond max_deviation)
    // Should trigger off-route
    let user_location_55m = UserLocation {
        coordinates: GeographicCoordinate { lng: 0.0005, lat: 0.0 }, // ~55m east
        horizontal_accuracy: 5.0,
        course_over_ground: None,
        timestamp: SystemTime::now(),
        speed: None,
    };

    let trip_state_55m = get_navigating_trip_state(
        user_location_55m.clone(),
        vec![current_route_step.clone()],
        vec![],
        RouteDeviation::NoDeviation, // Was on-route
    );

    // Should be off-route now
    let deviation_result = tracking.check_route_deviation(&route, &trip_state_55m);
    assert!(matches!(deviation_result, RouteDeviation::OffRoute { .. }));

    // Move back to 45m (between thresholds)
    // Should STAY off-route because 45m > on_route_threshold (40m)
    let trip_state_45m_from_offroute = get_navigating_trip_state(
        user_location_45m.clone(),
        vec![current_route_step.clone()],
        vec![],
        RouteDeviation::OffRoute { deviation_from_route_line: 55.0 }, // Was off-route
    );

    // Should remain off-route because 45m > on_route_threshold (40m)
    let deviation_result_2 = tracking.check_route_deviation(&route, &trip_state_45m_from_offroute);
    assert!(matches!(deviation_result_2, RouteDeviation::OffRoute { .. }));

    // Move to 35m (below on_route_threshold)
    // Should return to on-route
    let user_location_35m = UserLocation {
        coordinates: GeographicCoordinate { lng: 0.00031, lat: 0.0 }, // ~35m east
        horizontal_accuracy: 5.0,
        course_over_ground: None,
        timestamp: SystemTime::now(),
        speed: None,
    };

    let trip_state_35m = get_navigating_trip_state(
        user_location_35m.clone(),
        vec![current_route_step.clone()],
        vec![],
        RouteDeviation::OffRoute { deviation_from_route_line: 45.0 }, // Was off-route
    );

    // Should be back on-route because 35m <= on_route_threshold (40m)
    assert_eq!(
        tracking.check_route_deviation(&route, &trip_state_35m),
        RouteDeviation::NoDeviation
    );
}
