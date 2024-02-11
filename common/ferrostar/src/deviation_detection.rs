use crate::models::{Route, RouteStep, UserLocation};
use crate::algorithms::deviation_from_line;
use geo::Point;
use std::sync::Arc;

#[cfg(test)]
use {
    crate::{
        models::GeographicCoordinate,
        navigation_controller::test_helpers::{gen_dummy_route_step, gen_route_from_steps},
    },
    proptest::proptest,
    std::time::SystemTime,
};

#[derive(Clone, uniffi::Enum)]
pub enum RouteDeviationTracking {
    /// No checks will be done, and we assume the user is always following the route.
    None,
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
                if location.horizontal_accuracy < *minimum_horizontal_accuracy as f64 {
                    // Check if the deviation from the route line is within tolerance,
                    // after sanity checking that the positioning signal is within accuracy tolerance.
                    deviation_from_line(
                        &Point::from(location),
                        &current_route_step.get_linestring(),
                    )
                    .map_or(RouteDeviation::NoDeviation, |deviation| {
                        if deviation > *max_acceptable_deviation {
                            RouteDeviation::OffRoute {
                                deviation_from_route_line: deviation,
                            }
                        } else {
                            RouteDeviation::NoDeviation
                        }
                    })
                } else {
                    RouteDeviation::NoDeviation
                }
            }
            RouteDeviationTracking::Custom { detector } => {
                detector.check_route_deviation(location, route.clone(), current_route_step.clone())
            }
        }
    }
}

/// Status information that describes whether the user is proceeding according to the route or not.
///
/// Note that the name is intentionally a bit generic to allow for expansion of other states.
/// For example, we could conceivably add a "wrong way" status in the future.
#[derive(Debug, Copy, Clone, PartialEq, uniffi::Enum)]
pub enum RouteDeviation {
    /// The user is proceeding on course within the expected tolerances; everything is normal.
    NoDeviation,
    /// The user is off the expected route.
    OffRoute {
        /// The deviation from the route line, in meters.
        deviation_from_route_line: f64,
    },
}

#[uniffi::export(with_foreign)]
pub trait RouteDeviationDetector: Send + Sync {
    /// Determines whether the user is following the route correctly or not.
    ///
    /// NOTE: This function is merely for reporting the tracking status based on available information.
    /// A return value indicating that the user is off route does not necessarily mean
    /// that a new route will be recalculated immediately.
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
    /// Tests [RouteDeviationTracking::None] behavior,
    /// which never reports that the user is off route, even when they obviously are.
    #[test]
    fn no_deviation_tracking(
        x1 in -180f64..180f64, y1 in -90f64..90f64,
        x2 in -180f64..180f64, y2 in -90f64..90f64,
        x3 in -180f64..180f64, y3 in -90f64..90f64,
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
        };
        assert_eq!(
            tracking.check_route_deviation(user_location_on_route, &route, &current_route_step),
            RouteDeviation::NoDeviation,
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
        };
        assert_eq!(
            tracking.check_route_deviation(user_location_random, &route, &current_route_step),
            RouteDeviation::NoDeviation,
        );
    }

    /// Implements the same behavior as [RouteDeviationTracking::None]
    /// with user-supplied code.
    #[test]
    fn custom_no_deviation_mode(
        x1 in -180f64..180f64, y1 in -90f64..90f64,
        x2 in -180f64..180f64, y2 in -90f64..90f64,
        x3 in -180f64..180f64, y3 in -90f64..90f64,
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
        };
        assert_eq!(
            tracking.check_route_deviation(user_location_on_route, &route, &current_route_step),
            RouteDeviation::NoDeviation,
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
        };
        assert_eq!(
            tracking.check_route_deviation(user_location_random, &route, &current_route_step),
            RouteDeviation::NoDeviation,
        );
    }

    /// Custom behavior claiming that the user is always off the route.
    #[test]
    fn custom_always_off_route(
        x1 in -180f64..180f64, y1 in -90f64..90f64,
        x2 in -180f64..180f64, y2 in -90f64..90f64,
        x3 in -180f64..180f64, y3 in -90f64..90f64,
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
        };
        assert_eq!(
            tracking.check_route_deviation(user_location_on_route, &route, &current_route_step),
            RouteDeviation::OffRoute {
                deviation_from_route_line: 7.0
            },
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
        };
        assert_eq!(
            tracking.check_route_deviation(user_location_random, &route, &current_route_step),
            RouteDeviation::OffRoute {
                deviation_from_route_line: 7.0
            },
        );
    }

    /// Tests [RouteDeviationTracking::StaticThreshold] behavior,
    /// using [crate::algorithms::deviation_from_line]
    #[test]
    fn static_threshold_oracle_test(
        x1 in -180f64..180f64, y1 in -90f64..90f64,
        x2 in -180f64..180f64, y2 in -90f64..90f64,
        x3 in -180f64..180f64, y3 in -90f64..90f64,
        minimum_horizontal_accuracy in 1u16..100u16,
        max_acceptable_deviation in 1f64..100f64,
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
            // TODO: Test different accuracy values
            horizontal_accuracy: 0.0,
            course_over_ground: None,
            timestamp: SystemTime::now(),
        };
        assert_eq!(
            tracking.check_route_deviation(user_location_on_route, &route, &current_route_step),
            RouteDeviation::NoDeviation,
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
        };
        let deviation = deviation_from_line(&Point::from(coordinates), &current_route_step.get_linestring());
        match tracking.check_route_deviation(user_location_random, &route, &current_route_step) {
            RouteDeviation::NoDeviation => {
                assert!(deviation.unwrap() <= max_acceptable_deviation)
            }
            RouteDeviation::OffRoute{ deviation_from_route_line } => {
                assert_eq!(
                    deviation_from_route_line,
                    deviation.unwrap(),
                );
            }
        }
    }
}
