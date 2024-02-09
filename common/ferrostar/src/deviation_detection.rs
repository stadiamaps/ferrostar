use crate::models::{Route, RouteStep, UserLocation};
use crate::navigation_controller::algorithms::deviation_from_line;
use geo::Point;
use std::sync::Arc;

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
    fn check_route_deviation(
        &self,
        location: UserLocation,
        route: Route,
        current_route_step: RouteStep,
    ) -> RouteDeviation;
}

// TODO: Tests!