use serde::{Deserialize, Serialize};

#[cfg(all(feature = "uniffi", not(feature = "wasm_js")))]
use crate::{models::Route, navigation_controller::models::TripState, UtcDateTime};

#[cfg(all(feature = "uniffi", not(feature = "wasm_js")))]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
pub struct NavigationCachingConfig {
    pub cache_interval_seconds: i64,
    pub max_age_seconds: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[cfg(all(feature = "uniffi", not(feature = "wasm_js")))]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
pub struct NavigationSessionSnapshot {
    pub saved_at: UtcDateTime,
    pub route: Route,
    pub trip_state: Option<TripState>,
}
