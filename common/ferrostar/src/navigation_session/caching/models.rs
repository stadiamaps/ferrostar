#[cfg(feature = "uniffi")]
use serde::{Deserialize, Serialize};

#[cfg(feature = "uniffi")]
use crate::{UtcDateTime, models::Route, navigation_controller::models::TripState};

#[cfg(feature = "uniffi")]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
pub struct NavigationCachingConfig {
    pub cache_interval_seconds: i64,
    pub max_age_seconds: i64,
}

#[cfg(feature = "uniffi")]
#[cfg_attr(
    feature = "uniffi",
    derive(uniffi::Record, Debug, Clone, Serialize, Deserialize)
)]
pub struct NavigationSessionSnapshot {
    pub saved_at: UtcDateTime,
    pub route: Route,
    pub trip_state: Option<TripState>,
}
