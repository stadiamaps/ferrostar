use serde::{Deserialize, Serialize};

use crate::{models::Route, navigation_controller::models::NavigationControllerConfig};

struct NavigationCachingConfig {

}

enum NavigationCachingReason {
    TimeInterval,
    StepAdvance
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct NavigationCachingRecord {
    config: NavigationControllerConfig,
    route: Route,
    trip_state: TripState,
}
