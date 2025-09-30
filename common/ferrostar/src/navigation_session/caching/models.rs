use serde::{Deserialize, Serialize};

use crate::{models::Route, navigation_controller::models::TripState, UtcDateTime};

#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
pub struct NavigationCachingConfig {
    pub cache_interval_seconds: i64,
    pub max_age_seconds: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
pub struct NavigationSessionRecord {
    pub saved_at: UtcDateTime,
    pub route: Route,
    pub trip_state: Option<TripState>,
}
